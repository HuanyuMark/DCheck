package org.example.dcheck.impl;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Date: 2025/2/28
 *
 * @author 三石而立Sunsy
 */
@Slf4j
@SuppressWarnings("unused")
public class EmbeddedNeo4jRelevancyEngine extends AbstractParagraphRelevancyEngine {
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
    protected static final String EMBEDDING_FUC_PROPERTY = "_$$_embedding_func_$$_";
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
                setCodec(CodecProvider.getInstance()
                        .getCodecs()
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("manual set codec before init(), otherwise list " + Codec.class + " provider in classpath")));
            }

            ApiConfig apiConfig = ConfigProvider.getInstance().getApiConfig();

            try {
                tempDbms = new Neo4jDbms(Files.createTempDirectory("tmp_neo4j_dbms_" + (int) (System.currentTimeMillis() / Math.random())));
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
        var includeProperties = includeMetadata.isEmpty() ? ".*" : includeMetadata.stream().map(field -> {
            String property = field.trim();
            if (property.isEmpty()) {
                throw new IllegalArgumentException("field cannot be empty");
            }
            return "." + property;
        }).collect(Collectors.joining(","));
        var cypher = MessageFormat.format(
                """
                        MATCH (p: {PARAGRAPH_LABEL})
                        WHERE p.{DOCUMENT_ID_PROPERTY} != $selfDocumentId
                        CALL db.index.vector.queryNodes($VECTOR_INDEX,$topK,$embedding)
                        YIELD node,score
                        RETURN apoc.map.removeKey(node {{includeProperties}}, $VECTOR_PROPERTY) as node,score
                        """,
                Map.of(
                        "PARAGRAPH_LABEL", PARAGRAPH_LABEL.name(),
                        "DOCUMENT_ID_PROPERTY", DOCUMENT_ID_PROPERTY,
                        "includeProperties", includeProperties
                ));
        log.debug("QueryParagraphCypher:\n {}", cypher);
        return cypher;
    }

    protected static final String QueryEmbeddingCypher = MessageFormat.format(
            """
                    MATCH (p: {PARAGRAPH_LABEL})
                    WHERE p.{DOCUMENT_ID_PROPERTY} == $queryDocument
                    RETURN p.{VECTOR_PROPERTY} as {VECTOR_PROPERTY},p.{EMBEDDING_FUC_PROPERTY} as {EMBEDDING_FUC_PROPERTY}
                    """,
            Map.of(
                    "PARAGRAPH_LABEL", PARAGRAPH_LABEL.name(),
                    "VECTOR_PROPERTY", VECTOR_PROPERTY,
                    "DOCUMENT_ID_PROPERTY", DOCUMENT_ID_PROPERTY,
                    "EMBEDDING_FUC_PROPERTY", EMBEDDING_FUC_PROPERTY
            ));

    @Override
    public ParagraphRelevancyQueryResult queryParagraph(ParagraphRelevancyQuery query) {
        var collection = getCollection(query.getCollectionId());
        DocumentCollection documentCollection = getOrCreateDocumentCollection(query.getCollectionId());
        try (var tx = collection.beginTx()) {
            Stream<Embedding> queryEmbeddings;
            if (query.getParagraphs() == null) {
                queryEmbeddings = tx.execute(QueryEmbeddingCypher, Collections.singletonMap("queryDocument", query.getDocumentId()))
                        .stream()
                        .map(properties -> Embedding.from((float[]) properties.get(VECTOR_PROPERTY), (String) properties.get(EMBEDDING_FUC_PROPERTY)));
            } else {
                // do partition for batch
                var partitions = CollectionUtils.partition(query.getParagraphs(), PARAGRAPH_HANDLE_CHUNK_SIZE);
                queryEmbeddings = partitions.stream().flatMap(partition -> embed(partition.stream()).stream());
            }

            String cypher = getQueryParagraphCypher(query.getIncludeMetadata());
            var records = queryEmbeddings.map(embedding -> tx.execute(cypher,
                            Map.of(
                                    "embedding", embedding.asArray(),
                                    "selfDocumentId", query.getDocumentId(),
                                    "VECTOR_INDEX", VECTOR_INDEX,
                                    "topK", query.getTopK(),
                                    "VECTOR_PROPERTY", VECTOR_PROPERTY
                            ))
                    .stream()
                    .map(result -> {
                        @SuppressWarnings("unchecked")
                        var nodeProperties = ((Map<String, Object>) result.get("node"));
                        var flatProperties = nodeProperties.entrySet().stream().filter(kv -> kv.getValue() instanceof String)
                                .map(kv -> new AbstractMap.SimpleEntry<>(kv.getKey(), (((String) kv.getValue()))))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                        ParagraphMetadata metadata;
                        try {
                            var flatMetadata = new HashMap<>(flatProperties);
                            flatMetadata.remove(CONTENT_PROPERTY);
                            flatMetadata.remove(EMBEDDING_FUC_PROPERTY);
                            flatMetadata.remove(VECTOR_PROPERTY);
                            metadata = codec.convertTo(flatMetadata.entrySet().stream().map(e -> {
                                try {
                                    return new AbstractMap.SimpleEntry<>(e.getKey(), codec.deserialize(e.getKey(), Object.class));
                                } catch (IOException ex) {
                                    throw new IllegalStateException("deserialize metadata '" + e.getKey() + "=" + e.getValue() + "' fail: " + ex.getMessage(), ex);
                                }
                            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), ParagraphMetadata.class);
                        } catch (IOException e) {
                            throw new IllegalStateException("convert flatProperties to ParagraphMetadata fail: " + e.getMessage(), e);
                        }

                        var paragraphContent = ContentConvert.castToContent(flatProperties.get(CONTENT_PROPERTY));

                        if (metadata.getParagraphType() != BuiltinParagraphType.TEXT) {
                            throw new UnsupportedOperationException("unsupported paragraph type: " + metadata.getParagraphType());
                        }

                        var paragraph = new TextParagraph(
                                documentCollection,
                                () -> (TextContent) paragraphContent,
                                metadata
                        );

                        return new ParagraphRelevancyQueryResult.Record(paragraph, (double) result.get("score"));
                    }).toList()).toList();
            tx.commit();
            return new ParagraphRelevancyQueryResult(records);
        }
    }


    protected List<Embedding> embed(Stream<? extends Content> contents) {
        try {
            return embeddingFunction.embedDocuments(contents.map(ContentConvert::castToText).collect(Collectors.toList()));
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
                    node.setProperty(EMBEDDING_FUC_PROPERTY, embeddings.get(i).getEmbeddingFunction());
                    node.setProperty(VECTOR_PROPERTY, embeddings.get(i).asArray());
                    node.setProperty(CONTENT_PROPERTY, ContentConvert.castToText(record.getContent()));
                    // flat metadata would be great for neo4j match performance
                    // 不将metadata单独序列化存储到一个property中是为了留有使用neo4j查询功能查找metadata的余地
                    for (Map.Entry<String, Object> kv : record.getMetadata().entrySet()) {
                        if (kv.getKey().equals(DOCUMENT_ID_PROPERTY) || kv.getKey().equals(VECTOR_PROPERTY) || kv.getKey().equals(EMBEDDING_FUC_PROPERTY)) {
                            throw new IllegalArgumentException("metadata key '" + kv.getKey() + "' is reserved");
                        }
                        node.setProperty(kv.getKey(), codec.serialize(kv.getValue(), String.class));
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
            BiFunction<String, Object, Object> valueReader = (propertyKey, value) -> {
                try {
                    return codec.serialize(value, String.class);
                } catch (IOException e) {
                    throw new IllegalArgumentException("serialize '" + propertyKey + "=" + value + "' fail: " + e.getMessage(), e);
                }
            };
            for (Map.Entry<String, String> kv : delete.getMetadataMatchCondition().getEqs().entrySet()) {
                tx.findNodes(PARAGRAPH_LABEL, kv.getKey(), valueReader.apply(kv.getKey(), kv.getValue())).forEachRemaining(Node::delete);
            }
            for (Map.Entry<String, Collection<String>> kvs : delete.getMetadataMatchCondition().getIns().entrySet()) {
                for (String value : kvs.getValue()) {
                    tx.findNodes(PARAGRAPH_LABEL, kvs.getKey(), valueReader.apply(kvs.getKey(), value)).forEachRemaining(Node::delete);
                }
            }
            tx.commit();
        }
    }

    @Override
    public List<Boolean> hasDocument(DocumentIdQuery query) {
        var collection = getCollection(query.getCollectionId());
        try (Transaction tx = collection.beginTx()) {
            var res = query.getDocumentIds().stream().map(id -> tx.findNodes(PARAGRAPH_LABEL, DOCUMENT_ID_PROPERTY, id).stream().findFirst().map(n -> Boolean.TRUE).orElse(Boolean.FALSE)).toList();
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
        if (!init) return;
        synchronized (this) {
            if (!init) return;
            for (var collection : tempDocumentCollections) {
                try {
                    collection.close();
                } catch (Exception e) {
                    log.warn("encounter some problem in closing '" + getClass().getSimpleName() + "': close temp collection fail: {}", e.getMessage(), e);
                }
            }
            dbms.shutdown();
            tempDbms.destroy();
            init = false;
        }
    }
}
