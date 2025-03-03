package org.example.dcheck.impl;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.example.dcheck.api.*;
import org.example.dcheck.api.embedding.Embedding;
import org.example.dcheck.api.embedding.EmbeddingFunction;
import org.example.dcheck.common.util.CollectionUtils;
import org.example.dcheck.common.util.ContentConvert;
import org.example.dcheck.common.util.MessageFormat;
import org.example.dcheck.spi.CodecProvider;
import org.example.dcheck.spi.ConfigProvider;
import org.example.dcheck.spi.EmbeddingFuncMapProvider;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexSetting;
import org.neo4j.graphdb.schema.IndexSettingImpl;
import org.neo4j.graphdb.schema.IndexType;
import org.neo4j.kernel.impl.core.NodeEntity;
import org.springframework.util.StringUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
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
@Slf4j
@SuppressWarnings("unused")
public class EmbeddedNeo4jRelevancyEngine extends AbstractParagraphRelevancyEngine implements Closeable {
    /////
    //@see https://neo4j.com/docs/cypher-manual/current/indexes/semantic-indexes/vector-indexes/
    // here are builtin api config
    public static final String DB_ROOT = "relevancy-engine.embedded-neo4j.data-path";
    public static final String SIMILARITY_FUNCTION = "relevancy-engine.embedded-neo4j.config.similarity_function";
    public static final String QUANTIZATION_ENABLE = "relevancy-engine.embedded-neo4j.config.quantization.enable";
    public static final String HNSW_M = "relevancy-engine.embedded-neo4j.config.hnsw.m";
    public static final String HNSW_EF_CONSTRUCTION = "relevancy-engine.embedded-neo4j.config.hnsw.ef_construction";
    /////

    protected static final Label PARAGRAPH_LABEL = Label.label("Paragraph");
    protected static final String VECTOR_INDEX = "vector_index";
    protected static final String DOCUMENT_ID_INDEX = "document_id_index";
    protected static final String VECTOR_PROPERTY = "_$$_embedding_$$_";
    protected static final String CONTENT_PROPERTY = "_$$_content_$$_";
    public static final String DOCUMENT_ID_PROPERTY = "documentId";
    public static final int PARAGRAPH_HANDLE_CHUNK_SIZE = 5;


    protected Map<IndexSetting, Object> vectorIndexSettings = new HashMap<>();

    protected final Set<String> indexedCollections = Collections.newSetFromMap(new ConcurrentSkipListMap<>());

    protected ManageableGraphDatabaseService getCollection(String collectionId) {
        var collection = dbms.getOrCreateDatabase(collectionId);
        buildIndexes(collectionId, collection);
        return collection;
    }

    protected Neo4jDbms dbms;

    protected Neo4jDbms tempDbms;

    protected final Map<String, DocumentCollection> collections = new ConcurrentSkipListMap<>();

    @Getter
    @Setter
    @NonNull
    protected EmbeddingFunction embeddingFunction;

    @Getter
    protected Codec codec;

    public void setCodec(@NonNull Codec codec) {
        this.codec = codec;
    }

    @Override
    public void doInit() {
        if (init) {
            return;
        }
        synchronized (this) {
            if (init) {
                return;
            }

            if (codec == null) {
                codec = CodecProvider.getInstance()
                        .getCodecs()
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("manual set codec before init(), otherwise list " + Codec.class + " provider in classpath"));
            }

            ApiConfig apiConfig = ConfigProvider.getInstance().getApiConfig();

            try {
                tempDbms = new Neo4jDbms(Files.createTempDirectory("tmp_neo4j_dbms_" + System.currentTimeMillis()));
            } catch (IOException e) {
                throw new IllegalStateException("create temp dir fail: " + e.getMessage(), e);
            }
            var dbRoot = apiConfig.getProperty(DB_ROOT);
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

            String similarityFunc = apiConfig.getProperty(SIMILARITY_FUNCTION);
            String quantizationEnable = apiConfig.getProperty(QUANTIZATION_ENABLE);
            String hnswM = apiConfig.getProperty(HNSW_M);
            String hnswEfConstruction = apiConfig.getProperty(HNSW_EF_CONSTRUCTION);
            if (similarityFunc != null) {
                vectorIndexSettings.put(IndexSettingImpl.VECTOR_SIMILARITY_FUNCTION, similarityFunc);
            }
            if (quantizationEnable != null) {
                if (!"true".equalsIgnoreCase(quantizationEnable) && !"false".equalsIgnoreCase(quantizationEnable)) {
                    throw new IllegalArgumentException("invalid config '" + QUANTIZATION_ENABLE + "=" + quantizationEnable + "' require type 'Boolean'");
                }
                vectorIndexSettings.put(IndexSettingImpl.VECTOR_QUANTIZATION_ENABLED, Boolean.parseBoolean(quantizationEnable));
            }
            if (hnswM != null) {
                try {
                    vectorIndexSettings.put(IndexSettingImpl.VECTOR_HNSW_M, Integer.parseInt(hnswM));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("invalid config '" + HNSW_M + "=" + hnswM + "' require type 'Integer'", e);
                }
            }
            if (hnswEfConstruction != null) {
                try {
                    vectorIndexSettings.put(IndexSettingImpl.VECTOR_HNSW_EF_CONSTRUCTION, Integer.parseInt(hnswEfConstruction));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("invalid config '" + HNSW_EF_CONSTRUCTION + "=" + hnswEfConstruction + "' require type 'Integer'", e);
                }
            }

            var embeddingModel = apiConfig.getProperty(ApiConfig.EMBEDDING_MODEL_KEY, ApiConfig.DEFAULT_VALUE);
            embeddingFunction = EmbeddingFuncMapProvider.getInstance().getFunc(embeddingModel);

            try {
                embeddingFunction.init();
            } catch (Exception e) {
                throw new IllegalStateException("init embedding function fail: " + e.getMessage(), e);
            }

            init = true;
        }
    }

    protected String getQueryParagraphCypher(Collection<String> includeMetadata) {
        var candidateProperties = includeMetadata.isEmpty() ? "" : includeMetadata.stream().map(field -> {
            String property = field.trim();
            if (property.isEmpty()) {
                throw new IllegalArgumentException("field cannot be empty");
            }
            return "." + property;
        }).collect(Collectors.joining(","));
        var includeProperties = candidateProperties.isEmpty() ? ".*," : candidateProperties + ",";
        //TODO 新增一个在数据库中查询已存在paragraph.embeddings然后KNN查询的cypher
        /*
        * var cypher = MessageFormat.format(
    """
    MATCH (source: {PARAGRAPH_LABEL})
    WHERE source.$DOCUMENT_ID_PROPERTY = $targetDocumentId
    WITH source.$VECTOR_PROPERTY AS targetVector
    CALL db.index.vector.queryNodes($VECTOR_INDEX, $topK, targetVector)
    YIELD node, score
    WHERE node.$DOCUMENT_ID_PROPERTY != $selfDocumentId
    RETURN node {{{includeProperties}}}, score
    """,
    Map.of(
        "PARAGRAPH_LABEL", PARAGRAPH_LABEL.name(),
        "includeProperties", includeProperties,
        "VECTOR_PROPERTY", VECTOR_PROPERTY,
        "DOCUMENT_ID_PROPERTY", DOCUMENT_ID_PROPERTY
    ));
        * */
        var cypher = MessageFormat.format(
                """
                        MATCH (p: {PARAGRAPH_LABEL})
                        WHERE p.$DOCUMENT_ID_PROPERTY != $selfDocumentId
                        CALL db.index.vector.queryNodes($VECTOR_INDEX,$topK,$embedding)
                        YIELD node,score
                        RETURN node {{includeProperties}{VECTOR_PROPERTY}: null},score
                        """,
                Map.of(
                        "PARAGRAPH_LABEL", PARAGRAPH_LABEL.name(),
                        "includeProperties", includeProperties,
                        "VECTOR_PROPERTY", VECTOR_PROPERTY
                ));
        log.debug("QueryParagraphCypher:\n {}", cypher);
        return cypher;
    }

    @Override
    public ParagraphRelevancyQueryResult queryParagraph(ParagraphRelevancyQuery query) {
        var collection = getCollection(query.getCollectionId());
        DocumentCollection documentCollection = getOrCreateDocumentCollection(query.getCollectionId());
        // do partition for batch
        //TODO 需要处理为null情况
        assert query.getParagraphs() != null;
        var partitions = CollectionUtils.partition(query.getParagraphs(), PARAGRAPH_HANDLE_CHUNK_SIZE);
        try (var tx = collection.beginTx()) {
            var records = partitions.stream().flatMap(partition -> embed(partition.stream()).stream().map(embedding -> tx.execute(getQueryParagraphCypher(query.getIncludeMetadata()),
                            Map.of(
                                    "embedding", embedding.asArray(),
                                    "selfDocumentId", query.getDocumentId(),
                                    "DOCUMENT_ID_PROPERTY", DOCUMENT_ID_PROPERTY,
                                    "VECTOR_INDEX", VECTOR_INDEX,
                                    "topK", query.getTopK()
                            ))
                    .stream()
                    .map(result -> {
                        var nodeProperties = ((NodeEntity) result.get("node")).getAllProperties();
                        var flatProperties = nodeProperties.entrySet().stream().filter(kv -> kv.getValue() instanceof String)
                                .map(kv -> new AbstractMap.SimpleEntry<>(kv.getKey(), (((String) kv.getValue()))))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                        var metadata = (ParagraphMetadata) codec.convertTo(flatProperties, ParagraphMetadata.class);
                        var paragraphContent = (Content) ContentConvert.castToContent(codec.convertTo(flatProperties.get(CONTENT_PROPERTY), String.class));

                        if (metadata.getParagraphType() != BuiltinParagraphType.TEXT) {
                            throw new UnsupportedOperationException("unsupported paragraph type: " + metadata.getParagraphType());
                        }

                        var paragraph = new TextParagraph(
                                documentCollection,
                                metadata.getDocumentId(),
                                () -> (TextContent) paragraphContent,
                                metadata.getLocation()
                        );

                        return new ParagraphRelevancyQueryResult.Record(paragraph, (double) result.get("score"));
                    }).toList())).toList();
            tx.commit();
            return new ParagraphRelevancyQueryResult(records);
        }
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
        } catch (Exception e) {
            throw new IllegalStateException("embed content fail: " + e.getMessage(), e);
        }
    }

    @Override
    public void addParagraph(ParagraphRelevancyCreation creation) {
        var collection = getCollection(creation.getCollectionId());

        // do partition for batch
        var partitions = CollectionUtils.partition(creation.getBatch(), PARAGRAPH_HANDLE_CHUNK_SIZE);

        try (var tx = collection.beginTx()) {
            for (var partition : partitions) {
                var embeddings = embed(partition.stream().map(ParagraphRelevancyCreation.Record::getContent));

                for (int i = 0; i < partition.size(); i++) {
                    var record = partition.get(i);
                    Node node = tx.createNode(PARAGRAPH_LABEL);
                    node.setProperty(VECTOR_PROPERTY, embeddings.get(i).asArray());
                    node.setProperty(CONTENT_PROPERTY, codec.convertTo(ContentConvert.castToText(record.getContent()), String.class));
                    for (Map.Entry<String, Object> kv : record.getMetadata().entrySet()) {
                        node.setProperty(kv.getKey(), codec.convertTo(kv.getValue(), String.class));
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
                tx.findNodes(PARAGRAPH_LABEL, kv.getKey(), kv.getValue()).forEachRemaining(Node::delete);
            }
            for (Map.Entry<String, Collection<String>> kvs : delete.getMetadataMatchCondition().getIns().entrySet()) {
                for (String value : kvs.getValue()) {
                    tx.findNodes(PARAGRAPH_LABEL, kvs.getKey(), value).forEachRemaining(Node::delete);
                }
            }
            tx.commit();
        }
    }

    @Override
    public List<Boolean> hasDocument(DocumentIdQuery query) {
        var collection = getCollection(query.getCollectionId());
        try (Transaction tx = collection.beginTx()) {
            var res = query.getDocumentIds().stream().map(id-> tx.findNodes(PARAGRAPH_LABEL, DOCUMENT_ID_PROPERTY, id).stream().findFirst().map(n->Boolean.TRUE).orElse(Boolean.FALSE)).toList();
            tx.commit();
            return res;
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
            tempDbms.dropDatabase(collectionId);
        } catch (IOException e) {
            throw new IllegalStateException("remove document collection fail: " + e.getMessage(), e);
        }
        indexedCollections.remove(collectionId);
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
                tx.schema()
                        .indexFor(PARAGRAPH_LABEL)
                        .withName(VECTOR_INDEX)
                        .withIndexType(IndexType.VECTOR)
                        .withIndexConfiguration(vectorIndexSettings)
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

    @Override
    protected DocumentCollection doNewTempDocumentCollection() {
        String collectionId = generateTempDocumentCollectionId();
        var collection = tempDbms.getOrCreateDatabase(collectionId);
        buildIndexes(collectionId, collection);
        return new EngineAdaptedDocumentCollection(collectionId, this);
    }

    protected void ensureOpen() {
        init();
    }

    @Override
    public void close() {
        ensureOpen();
        for (TempDocumentCollection collection : tempDocumentCollections) {
            try {
                collection.close();
            } catch (Exception e) {
                log.warn("encounter some problem in closing '" + getClass().getSimpleName() + "': close temp collection fail: {}", e.getMessage(), e);
            }
        }
        dbms.shutdown();
        tempDbms.shutdown();
    }
}
