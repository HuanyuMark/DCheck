package org.example.dcheck.impl;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.ExtensionMethod;
import org.apache.commons.io.IOUtils;
import org.example.dcheck.api.*;
import org.example.dcheck.common.util.CollectionUtils;
import org.example.dcheck.embedding.EmbeddingFunction;
import org.example.dcheck.spi.ConfigProvider;
import org.example.dcheck.spi.MapSpi;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexSettingImpl;
import org.neo4j.graphdb.schema.IndexType;
import org.neo4j.kernel.impl.coreapi.Neo4jTransaction;
import org.springframework.util.StringUtils;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Date: 2025/2/28
 *
 * @author 三石而立Sunsy
 */
@ExtensionMethod(Neo4jTransaction.class)
public class EmbeddedNeo4jRelevancyEngine extends AbstractParagraphRelevancyEngine implements Closeable {
    public static final String DB_ROOT = "db.vector.embedded-neo4j.data-path";
    protected Neo4jDbms dbms;
    protected final Map<String, DocumentCollection> collections = new ConcurrentSkipListMap<>();

    @Getter
    @Setter
    @NonNull
    protected EmbeddingFunction embeddingFunction;

    @Getter
    @Setter
    @NonNull
    protected Gson gson;

    @Override
    public void doInit() {
        if (init) {
            return;
        }
        synchronized (this) {
            if (init) {
                return;
            }
            var dbRoot = ConfigProvider.getInstance().getApiConfig().getProperty(DB_ROOT);
            if (!StringUtils.hasText(dbRoot)) {
                throw new IllegalStateException("invalid config '" + DB_ROOT + "=" + dbRoot + "'");
            }
            Path dbRootPath;
            try {
                dbRootPath = Paths.get(dbRoot);
            } catch (Exception e) {
                throw new IllegalStateException("invalid config '" + DB_ROOT + "=" + dbRoot + "': " + e.getMessage(), e);
            }
            dbms = new Neo4jDbms(dbRootPath);

            var embeddingModel = ConfigProvider.getInstance().getApiConfig().getProperty(ApiConfig.EMBEDDING_MODEL_KEY, ApiConfig.DEFAULT_VALUE);
            embeddingFunction = MapSpi.getInstance().getFunc(embeddingModel);

            try {
                embeddingFunction.init();
            } catch (Exception e) {
                throw new IllegalStateException("init embedding function fail: " + e.getMessage(), e);
            }

            init = true;
        }
    }

    protected static final String[] EMPTY_STRING_ARRAY = new String[0];


    @Override
    public ParagraphRelevancyQueryResult queryParagraph(ParagraphRelevancyQuery query) {
        var collection = getCollection(query.getCollectionId());
        // do partition for batch
        var partitions = CollectionUtils.partition(query.getParagraphs(), 5);
        try (var tx = (Neo4jTransaction) collection.beginTx()) {
            for (List<? extends Content> partition : partitions) {
                List<Embedding> embeddings = embed(partition.stream());
                embeddings.stream().map(embedding -> {
                    //TODO 反序列node为paragraph
                    tx.findNearestNeighbors(PARAGRAPH_LABEL, VECTOR_PROPERTY, embedding.asArray(), query.getTopK()).stream().limit(query.getTopK())
                            .map(n -> new AbstractMap.SimpleEntry<>(" ", n.getProperties(query.getIncludeMetadata().toArray(EMPTY_STRING_ARRAY))));
                    return null;
                });
            }
//            tx.traversalDescription()
//            query.get
            tx.commit();
        }
        return null;
    }


    protected List<Embedding> embed(Stream<? extends Content> contents) {
        try {
            return embeddingFunction.embedDocuments(contents.map(c -> {
                if (c instanceof InMemoryTextContent) return ((InMemoryTextContent) c).getText().toString();
                if (c instanceof TextContent) {
                    try {
                        return new String(IOUtils.toByteArray(c.getInputStream()));
                    } catch (IOException e) {
                        throw new IllegalStateException("read text content fail:", e);
                    }
                }
                //TODO support other types
                throw new UnsupportedOperationException();
            }).collect(Collectors.toList()));
        } catch (EFException e) {
            throw new IllegalStateException("embed content fail: " + e.getMessage(), e);
        }
    }

    @Override
    public void addParagraph(ParagraphRelevancyCreation creation) {
        var collection = getCollection(creation.getCollectionId());

        // do partition for batch
        var partitions = CollectionUtils.partition(creation.getBatch(), 5);

        try (var tx = collection.beginTx()) {
            for (var partition : partitions) {
                var embeddings = embed(partition.stream().map(ParagraphRelevancyCreation.Record::getContent));

                for (int i = 0; i < partition.size(); i++) {
                    var record = partition.get(i);
                    Node node = tx.createNode(PARAGRAPH_LABEL);
                    node.setProperty(VECTOR_PROPERTY, embeddings.get(i).asArray());
                    //TODO 设置 CONTENT_PROPERTY 保存paragraph内容
//                    node.setProperty(CONTENT_PROPERTY, );
                    for (Map.Entry<String, Object> kv : record.getMetadata().entrySet()) {
                        node.setProperty(kv.getKey(), gson.toJson(kv.getValue()));
                    }
                }
            }
            tx.commit();
        } catch (Exception e) {
            throw new IllegalStateException("add paragraph fail: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeDocument(DocumentDelete delete) {
        var collection = getCollection(delete.getCollectionId());
        try (Transaction tx = collection.beginTx()) {
            for (Map.Entry<String, String> kv : delete.getMetadataMatchCondition().getEqs().entrySet()) {
                tx.findNodes(PARAGRAPH_LABEL, kv.getKey(), kv.getValue());
            }
            for (Map.Entry<String, Collection<String>> kvs : delete.getMetadataMatchCondition().getIns().entrySet()) {
                for (String value : kvs.getValue()) {
                    tx.findNodes(PARAGRAPH_LABEL, kvs.getKey(), value);
                }
            }
            tx.commit();
        }
    }

    @Override
    public DocumentCollection getOrCreateDocumentCollection(String collectionId) {
        return collections.computeIfAbsent(collectionId, id -> new EngineAdaptedDocumentCollection(getCollection(id).databaseName(), this));
    }

    @Override
    public void removeDocumentCollection(String collectionId) {
        ensureOpen();
        try {
            dbms.dropDatabase(collectionId);
        } catch (IOException e) {
            throw new IllegalStateException("remove document collection fail: " + e.getMessage(), e);
        }
        indexedCollections.remove(collectionId);
    }

    protected static final Label PARAGRAPH_LABEL = Label.label("Paragraph");
    protected static final String VECTOR_INDEX = "vector_index";
    protected static final String DOCUMENT_ID_INDEX = "document_id_index";
    protected static final String VECTOR_PROPERTY = "_$$_embedding_$$_";
    protected static final String CONTENT_PROPERTY = "_$$_content_$$_";
    public static final String DOCUMENT_ID_PROPERTY = "documentId";
//    public static final

    protected final Set<String> indexedCollections = Collections.newSetFromMap(new ConcurrentSkipListMap<>());

    protected ManageableGraphDatabaseService getCollection(String collectionId) {
        var collection = dbms.getOrCreateDatabase(collectionId);
        buildIndexes(collectionId, collection);
        return collection;
    }

    protected void buildIndexes(String collectionId, ManageableGraphDatabaseService collection) {
        if (!indexedCollections.add(collectionId)) {
            return;
        }
        try (var tx = collection.beginTx()) {
            try {
                // try get vector index
                tx.schema().getIndexByName(VECTOR_INDEX);
            } catch (IllegalArgumentException e) {
                // fail => none => create
                //@see https://neo4j.com/docs/cypher-manual/current/indexes/semantic-indexes/vector-indexes/
                // Prior to Neo4j 5.23, the OPTIONS map was mandatory since a vector index could not be created
                // without setting the vector dimensions and similarity function.
                // Since Neo4j 5.23, both can be omitted.
                //  IndexSettingImpl.VECTOR_DIMENSIONS => 1024
                tx.schema()
                        .indexFor(PARAGRAPH_LABEL)
                        .withName(VECTOR_INDEX)
                        .withIndexType(IndexType.VECTOR)
                        .withIndexConfiguration(Collections.singletonMap(IndexSettingImpl.VECTOR_SIMILARITY_FUNCTION, "cosine"))
                        .on(VECTOR_PROPERTY)
                        .create();
            }
            try {
                // try get document id index
                tx.schema().getIndexByName(DOCUMENT_ID_INDEX);
            } catch (IllegalArgumentException e) {
                tx.schema()
                        .indexFor(PARAGRAPH_LABEL)
                        .withName(DOCUMENT_ID_INDEX)
                        .withIndexType(IndexType.RANGE)
                        .on(DOCUMENT_ID_PROPERTY)
                        .create();
            }
            tx.commit();
        } catch (Throwable e) {
            indexedCollections.remove(collectionId);
            throw e;
        }
    }

    protected void ensureOpen() {
        init();
    }

    @Override
    public void close() throws IOException {
        ensureOpen();
        dbms.shutdown();
    }
}
