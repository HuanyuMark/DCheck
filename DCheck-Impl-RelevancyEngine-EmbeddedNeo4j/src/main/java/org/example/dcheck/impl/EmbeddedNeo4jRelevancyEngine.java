package org.example.dcheck.impl;

import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.api.*;
import org.example.dcheck.api.embedding.Embedding;
import org.example.dcheck.api.embedding.EmbeddingFunction;
import org.example.dcheck.common.util.CollectionUtils;
import org.example.dcheck.common.util.ContentConvert;
import org.example.dcheck.common.util.MessageFormat;
import org.example.dcheck.spi.CodecProvider;
import org.example.dcheck.spi.DCheckConfigProvider;
import org.example.dcheck.spi.EmbeddingFuncMapProvider;
import org.example.dcheck.util.UtilConst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexSetting;
import org.neo4j.graphdb.schema.IndexSettingImpl;
import org.neo4j.graphdb.schema.IndexType;
import org.neo4j.kernel.impl.core.NodeEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Date: 2025/2/28
 *
 * @author 三石而立Sunsy
 */
@Slf4j
@SuppressWarnings("unused")
public class EmbeddedNeo4jRelevancyEngine extends AbstractParagraphRelevancyEngine {
    public static final String DOCUMENT_ID_PROPERTY = "documentId";
    public static final int PARAGRAPH_HANDLE_CHUNK_SIZE = 50;
    protected static final Label PARAGRAPH_LABEL = Label.label("Paragraph");
    protected static final String VECTOR_INDEX = "vector_index";
    protected static final String DOCUMENT_ID_INDEX = "document_id_index";
    protected static final String VECTOR_PROPERTY = "_$$_embedding_$$_";
    protected static final String CONTENT_PROPERTY = "_$$_content_$$_";
    protected static final String EMBEDDING_FUC_PROPERTY = "_$$_embedding_func_$$_";
    protected static final Label COLLECTION_METADATA_LABEL = Label.label("CollectionMetadata");
    protected static final String EMBEDDING_FUC_DETAILS_PROPERTY = "_$$_embedding_func_details_$$_";
    protected static final String QueryEmbeddingCypher = MessageFormat.format(
            """
                    MATCH (p: {PARAGRAPH_LABEL})
                    WHERE p.{DOCUMENT_ID_PROPERTY} = $queryDocument
                    RETURN p
                    """,
            Map.of(
                    "PARAGRAPH_LABEL", PARAGRAPH_LABEL.name(),
                    "VECTOR_PROPERTY", VECTOR_PROPERTY,
                    "DOCUMENT_ID_PROPERTY", DOCUMENT_ID_PROPERTY
            ));
    protected final Set<String> initedCollections = Collections.newSetFromMap(new ConcurrentSkipListMap<>());
    protected final Map<String, DocumentCollection> collections = new ConcurrentSkipListMap<>();
    protected static final String QUERY_PARAGRAPH_CYPHER =
            """
                    CALL db.index.vector.queryNodes($VECTOR_INDEX,$topK,$embedding)
                    YIELD node,score
                    RETURN node,score
                    """;
    @Nullable
    protected Map<IndexSetting, Object> vectorIndexSettings;
    protected Neo4jDbms dbms;
    protected Neo4jDbms tempDbms;
    @Getter
    protected EmbeddingFunction embeddingFunction;
    @Getter
    protected Codec codec;

    @NotNull
    private static String[] filterVectorPropertyKey(Node node) {
        return StreamSupport.stream(node.getPropertyKeys().spliterator(), false)
                .filter(k -> !VECTOR_PROPERTY.equals(k)).toArray(String[]::new);
    }

    public void setCodec(@NonNull Codec codec) {
        this.codec = codec;
    }

    protected ManageableGraphDatabaseService getCollection(String collectionId) {
        var collection = dbms.getOrCreateDatabase(collectionId);
        initCollectionIfNeeded(collectionId, collection);
        return collection;
    }

    public void setEmbeddingFunction(@NonNull EmbeddingFunction embeddingFunction) {
        if (this.embeddingFunction != null) {
            if (!embeddingFunction.equals(this.embeddingFunction) && !Objects.equals(embeddingFunction.getDetails(), this.embeddingFunction.getDetails())) {
                initedCollections.clear();
            }
        }
        this.embeddingFunction = embeddingFunction;
    }

    @NonNull
    protected Map<IndexSetting, Object> getVectorIndexSettings() {
        return vectorIndexSettings == null ? (vectorIndexSettings = new HashMap<>()) : vectorIndexSettings;
    }


    @Override
    public void doInit() {
        if (codec == null) {
            setCodec(CodecProvider.getInstance()
                    .getCodecs()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("manual set codec before init(), otherwise list " + Codec.class + " provider in classpath")));
        }

        DCheckConfig DCheckConfig = DCheckConfigProvider.getInstance().getDCheckConfig();

        try {
            tempDbms = new Neo4jDbms(Files.createTempDirectory("tmp_neo4j_dbms_" + (int) (System.currentTimeMillis() / Math.random())));
        } catch (IOException e) {
            throw new IllegalStateException("create temp dir fail: " + e.getMessage(), e);
        }
        var dbRootPath = DCheckConfig.required(EmbeddedNeo4jConfigKey.DB_ROOT, Path.class);
        dbms = new Neo4jDbms(dbRootPath);

        String similarityFunc = DCheckConfig.nullable(EmbeddedNeo4jConfigKey.SIMILARITY_FUNCTION);
        Boolean quantizationEnable = DCheckConfig.nullableEnable(EmbeddedNeo4jConfigKey.QUANTIZATION_ENABLE);
        Integer hnswM = DCheckConfig.nullablePositiveInt(EmbeddedNeo4jConfigKey.HNSW_M);
        Integer hnswEfConstruction = DCheckConfig.nullablePositiveInt(EmbeddedNeo4jConfigKey.HNSW_EF_CONSTRUCTION);
        if (similarityFunc != null) {
            getVectorIndexSettings().put(IndexSettingImpl.VECTOR_SIMILARITY_FUNCTION, similarityFunc);
        }
        if (quantizationEnable != null) {
            getVectorIndexSettings().put(IndexSettingImpl.VECTOR_QUANTIZATION_ENABLED, quantizationEnable);
        }
        if (hnswM != null) {
            getVectorIndexSettings().put(IndexSettingImpl.VECTOR_HNSW_M, hnswM);
        }
        if (hnswEfConstruction != null) {
            try {
                getVectorIndexSettings().put(IndexSettingImpl.VECTOR_HNSW_EF_CONSTRUCTION, hnswEfConstruction);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid config '" + EmbeddedNeo4jConfigKey.HNSW_EF_CONSTRUCTION + "=" + hnswEfConstruction + "' require type 'Integer'", e);
            }
        }

        var embeddingModel = DCheckConfig.required(DCheckConfig.EMBEDDING_MODEL_KEY, DCheckConfig.DEFAULT_VALUE);
        embeddingFunction = EmbeddingFuncMapProvider.getInstance().getFunc(embeddingModel);

        try {
            embeddingFunction.init();
        } catch (Exception e) {
            throw new IllegalStateException("init embedding function fail: " + e.getMessage(), e);
        }
    }

    @Override
    public void inited() throws Exception {
        super.inited();
        embeddingFunction.inited();
    }


    @Override
    public ParagraphRelevancyQueryResult queryParagraph(ParagraphRelevancyQuery _query) {

        ParagraphRelevancyQuery query;
        try {
            query = _query.withDocumentId(codec.serialize(_query.getDocumentId(), String.class));
        } catch (IOException e) {
            throw new IllegalStateException("serialize documentId '" + _query.getDocumentId() + "' fail: " + e.getMessage(), e);
        }

        var collection = getCollection(query.getCollectionId());
        DocumentCollection documentCollection = getOrCreateDocumentCollection(query.getCollectionId());
        var preparedEmbeddingArguments = prepareEmbeddingArguments(query, collection);
        List<? extends Map.Entry<UniversalParagraph, Embedding>> embeddingArguments = preparedEmbeddingArguments.getArguments();
        List<Map<String, Object>> selfParagraphs;

        try (var tx = preparedEmbeddingArguments.getTx() == null ? collection.beginTx() : preparedEmbeddingArguments.getTx()) {
            // exclude self: delete self paragraphs in the graph: current neo4j did`t support pre-filtering KNN
            selfParagraphs = tx.findNodes(PARAGRAPH_LABEL, DOCUMENT_ID_PROPERTY, query.getDocumentId())
                    .stream().map(n -> {
                        Map<String, Object> properties = n.getAllProperties();
                        n.delete();
                        return properties;
                    }).toList();

            // await vector index online after delete self paragraphs(we mutate the indexed data before)
            System.out.println(tx.schema().getIndexState(tx.schema().getIndexByName(VECTOR_INDEX)));
            tx.schema().awaitIndexOnline(VECTOR_INDEX, 5, TimeUnit.SECONDS);

            var duplicateParts = embeddingArguments.stream().map(entry -> {
                Paragraph diffTarget;

                UniversalParagraph universalParagraph = entry.getKey();
                if (universalParagraph.getParagraphType() == BuiltinParagraphType.TEXT) {
                    diffTarget = new TextParagraph(() -> (TextContent) universalParagraph.getContent(), universalParagraph.getMetadata());
                } else {
                    throw new UnsupportedOperationException("unsupported paragraph type: " + entry.getKey().getParagraphType());
                }


                var duplicates = tx.execute(QUERY_PARAGRAPH_CYPHER,
                                Map.of(
                                        "embedding", entry.getValue().asArray(),
                                        "VECTOR_INDEX", VECTOR_INDEX,
                                        "topK", query.getTopK()
                                ))
                        .stream()
                        .map(result -> {
                            NodeEntity node = (NodeEntity) result.get("node");

                            String[] filteredPropertyKeys = filterVectorPropertyKey(node);

                            var nodeProperties = (node).getProperties(filteredPropertyKeys);

                            return new DuplicatePart.DuplicateParagraph(mapToParagraph(nodeProperties), (double) result.get("score"));
                        }).toList();

                return new DuplicatePart(diffTarget, duplicates);
            }).toList();

            // restore self
            for (var selfParagraph : selfParagraphs) {
                Node node = tx.createNode(PARAGRAPH_LABEL);
                for (var kv : selfParagraph.entrySet()) {
                    node.setProperty(kv.getKey(), kv.getValue());
                }
            }
            tx.commit();
            return new ParagraphRelevancyQueryResult(duplicateParts);
        }
    }

    @NotNull
    protected EmbeddingArguments prepareEmbeddingArguments(ParagraphRelevancyQuery query, ManageableGraphDatabaseService collection) {
        List<? extends Map.Entry<UniversalParagraph, Embedding>> embeddingArguments;
        if (query.getParagraphs() == null) {
            var tx = collection.beginTx();
            try {
                embeddingArguments = tx.execute(QueryEmbeddingCypher, Collections.singletonMap("queryDocument", query.getDocumentId()))
                        .stream()
                        .map(result -> {
                            Map<String, Object> properties = ((NodeEntity) result.get("p")).getAllProperties();
                            Paragraph paragraph = mapToParagraph(properties);
                            return new AbstractMap.SimpleEntry<>(new UniversalParagraph(new SimpleParagraph(paragraph::getContent, paragraph.getMetadata().getParagraphType(), paragraph.getMetadata().getLocation()),
                                    paragraph.getMetadata()),
                                    Embedding.from((float[]) properties.get(VECTOR_PROPERTY)));
                        }).toList();
                return new EmbeddingArguments(embeddingArguments, tx);
            } catch (Throwable e) {
                tx.rollback();
                throw e;
            }
        }
        // do partition for batch
        var partitions = CollectionUtils.partition(query.getParagraphs(), PARAGRAPH_HANDLE_CHUNK_SIZE);
        embeddingArguments = partitions.stream().flatMap(partition -> {
            var embeddings = embed(partition.stream().map(UniversalParagraph::getContent));
            return IntStream.range(0, partition.size())
                    .mapToObj(i -> new AbstractMap.SimpleEntry<>(partition.get(i), embeddings.get(i)));
        }).toList();
        return new EmbeddingArguments(embeddingArguments, null);
    }

    @Override
    public void addParagraph(ParagraphRelevancyCreation creation) {
        var collection = getCollection(creation.getCollectionId());

        // do partition for batch
        var partitions = CollectionUtils.partition(creation.getBatch(), PARAGRAPH_HANDLE_CHUNK_SIZE);

        try (var tx = collection.beginTx()) {
            for (var partition : partitions) {
                var embeddings = embed(partition.stream().map(UniversalParagraph::getContent));

                for (int i = 0; i < partition.size(); i++) {
                    var record = partition.get(i);
                    Node node = tx.createNode(PARAGRAPH_LABEL);
                    node.setProperty(VECTOR_PROPERTY, embeddings.get(i).asArray());
                    node.setProperty(CONTENT_PROPERTY, ContentConvert.castToText(record.getContent()));
                    // flat metadata would be great for neo4j match performance
                    // 不将metadata单独序列化存储到一个property中是为了留有使用neo4j查询功能查找metadata的余地
                    for (Map.Entry<String, String> kv : record.getMetadata().toFlatMap(obj -> {
                        try {
                            return codec.serialize(obj, String.class);
                        } catch (IOException e) {
                            throw new IllegalArgumentException("serialize '" + obj + "' fail: " + e.getMessage(), e);
                        }
                    }).entrySet()) {
                        if (kv.getKey().equals(VECTOR_PROPERTY) || kv.getKey().equals(CONTENT_PROPERTY)) {
                            throw new IllegalArgumentException("metadata key '" + kv.getKey() + "' is reserved");
                        }
                        node.setProperty(kv.getKey(), kv.getValue());
                    }
                }
            }
            tx.commit();
        } catch (Throwable e) {
            throw new IllegalStateException("add paragraph fail: " + e.getMessage(), e);
        }
    }

    protected Paragraph mapToParagraph(Map<String, Object> nodeProperties) {
        var flatProperties = nodeProperties.entrySet().stream().filter(kv -> kv.getValue() instanceof String)
                .map(kv -> new AbstractMap.SimpleEntry<>(kv.getKey(), (((String) kv.getValue()))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        ParagraphMetadata metadata;
        try {
            flatProperties.remove(CONTENT_PROPERTY);
            flatProperties.remove(VECTOR_PROPERTY);
            metadata = codec.convertTo(flatProperties.entrySet().stream().map(e -> {
                try {
                    return new AbstractMap.SimpleEntry<>(e.getKey(), codec.deserialize(e.getValue(), Object.class));
                } catch (IOException ex) {
                    throw new IllegalStateException("deserialize metadata '" + e.getKey() + "=" + e.getValue() + "' fail: " + ex.getMessage(), ex);
                }
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), ParagraphMetadata.class);
        } catch (IOException e) {
            throw new IllegalStateException("convert flatProperties to ParagraphMetadata fail: " + e.getMessage(), e);
        }

        var paragraphContent = ContentConvert.castToContent(nodeProperties.get(CONTENT_PROPERTY));

        if (metadata.getParagraphType() != BuiltinParagraphType.TEXT) {
            throw new UnsupportedOperationException("unsupported paragraph type: " + metadata.getParagraphType());
        }

        return new TextParagraph(
                () -> (TextContent) paragraphContent,
                metadata
        );
    }


    protected List<Embedding> embed(Stream<? extends Content> contents) {
        try {
            return embeddingFunction.embedDocuments(contents.map(ContentConvert::castToText).collect(Collectors.toList()));
        } catch (Exception e) {
            throw new IllegalStateException("embed content fail: " + e.getMessage(), e);
        }
    }

    @Value
    @NonFinal
    protected static class EmbeddingArguments {
        List<? extends Map.Entry<UniversalParagraph, Embedding>> arguments;
        @Nullable
        Transaction tx;
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
            for (Map.Entry<String, Set<String>> kvs : delete.getMetadataMatchCondition().getIns().entrySet()) {
                for (String value : kvs.getValue()) {
                    tx.findNodes(PARAGRAPH_LABEL, kvs.getKey(), valueReader.apply(kvs.getKey(), value)).forEachRemaining(Node::delete);
                }
            }
//            delete.getMetadataMatchCondition()
            tx.commit();
        }
    }

    @Override
    public List<Boolean> hasDocument(DocumentIdQuery query) {
        var collection = getCollection(query.getCollectionId());
        try (Transaction tx = collection.beginTx()) {
            var res = query.getDocumentIds().stream().map(id -> {
                try {
                    return tx.findNodes(PARAGRAPH_LABEL, DOCUMENT_ID_PROPERTY, codec.serialize(id, String.class)).stream().findFirst().map(n -> Boolean.TRUE).orElse(Boolean.FALSE);
                } catch (IOException e) {
                    throw new IllegalStateException("serialize '" + DOCUMENT_ID_PROPERTY + "=" + id + "' fail: " + e.getMessage(), e);
                }
            }).toList();
            tx.commit();
            return res;
        }
    }

    @Override
    public List<Paragraph> getParagraphs(ParagraphGet query) {
        var collection = getCollection(query.getCollectionId());

        var condition = query.getCondition();
        if (condition == null || query.getCondition().getEqs().isEmpty() && query.getCondition().getIns().isEmpty() && query.getCondition().getNes().isEmpty() && query.getCondition().getNins().isEmpty()) {
            try (var tx = collection.beginTx()) {
                var ns = tx.findNodes(PARAGRAPH_LABEL).stream().map(node -> mapToParagraph(node.getProperties(filterVectorPropertyKey(node)))).toList();
                tx.commit();
                return ns;
            }
        }

        var cypherBuilder = new StringBuilder("MATCH (p:").append(PARAGRAPH_LABEL.name()).append(") ").append("WHERE ");
        var arguments = new CypherArgument();

        var eqs = condition
                .getEqs()
                .entrySet()
                .stream()
                .map(entry -> entry.getKey() + " = " + arguments.addSingle(entry.getValue()));

        var nes = condition.getNes().entrySet()
                .stream()
                .map(entry -> entry.getKey() + " != " + arguments.addSingle(entry.getValue()));

        var ins = condition.getIns().entrySet().stream().map(entry -> new StringBuilder(entry.getKey()).append(" IN ").append(arguments.addList(entry.getValue())));

        var nins = condition.getIns().entrySet().stream().map(entry -> " NOT " + entry.getKey() + " IN " + arguments.addList(entry.getValue()));

        String whereStatement = Stream.concat(eqs, Stream.concat(nes, Stream.concat(ins, nins))).collect(Collectors.joining(" AND "));

        cypherBuilder
                .append(whereStatement)
                .append(" RETURN p");

        arguments.put("$VECTOR_PROPERTY", VECTOR_PROPERTY);

        try (var tx = collection.beginTx()) {
            var ns = tx.execute(cypherBuilder.toString(), arguments)
                    .stream()
                    .map(result -> {
                        Node node = (Node) result.get("p");
                        Map<String, Object> p = node.getProperties(filterVectorPropertyKey(node));
                        return mapToParagraph(p);
                    })
                    .collect(Collectors.toList());
            tx.commit();
            return ns;
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
        initedCollections.remove(collectionId);
    }

    protected Transaction initIndex(String collectionId, ManageableGraphDatabaseService collection) {
        Transaction tx = collection.beginTx();
        try {
            boolean schemaUpdated = false;
            try {
                // try get vector index
                tx.schema().getIndexByName(VECTOR_INDEX);
            } catch (IllegalArgumentException e) {
                // fail => none => create
                var creator = tx.schema()
                        .indexFor(PARAGRAPH_LABEL)
                        .withName(VECTOR_INDEX)
                        .withIndexType(IndexType.VECTOR);

                if (vectorIndexSettings != null) {
                    creator = creator
                            .withIndexConfiguration(vectorIndexSettings);
                }

                creator.on(VECTOR_PROPERTY)
                        .create();

                schemaUpdated = true;
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
                schemaUpdated = true;
            }

            if (schemaUpdated) {
                tx.commit();
                return collection.beginTx();
            }
            return tx;
        } catch (Throwable e) {
            tx.rollback();
            throw e;
        }
    }

    protected void initCollectionIfNeeded(String collectionId, ManageableGraphDatabaseService collection) {
        if (!initedCollections.add(collectionId)) {
            return;
        }
        try (var tx = initIndex(collectionId, collection)) {
            tx.findNodes(COLLECTION_METADATA_LABEL).stream().findFirst().ifPresentOrElse(node -> {
                var properties = node.getAllProperties();
                if (!properties.get(EMBEDDING_FUC_PROPERTY).equals(embeddingFunction.getName())) {
                    throw new IllegalStateException("collection '" + collectionId + "' embedding function is not match(old != current): " + properties.get(EMBEDDING_FUC_PROPERTY) + " != " + embeddingFunction.getName());
                }
                Map<String, Object> details;
                try {
                    details = codec.deserialize(properties.get(EMBEDDING_FUC_DETAILS_PROPERTY), UtilConst.MAP_TYPE);
                } catch (IOException e) {
                    throw new IllegalStateException("deserialize embedding function state fail: " + e.getMessage(), e);
                }
                if (!Objects.equals(embeddingFunction.getDetails(), details)) {
                    throw new IllegalStateException("collection '" + collectionId + "' embedding function state is not match(old != current): " + details + " != " + embeddingFunction.getDetails());
                }
            }, () -> {
                Node metadataNode = tx.createNode(COLLECTION_METADATA_LABEL);
                metadataNode.setProperty(EMBEDDING_FUC_PROPERTY, embeddingFunction.getName());
                try {
                    metadataNode.setProperty(EMBEDDING_FUC_DETAILS_PROPERTY, codec.serialize(embeddingFunction.getDetails(), String.class));
                } catch (IOException e) {
                    throw new IllegalStateException("serialize metadata '" + EMBEDDING_FUC_DETAILS_PROPERTY + "=" + embeddingFunction.getDetails() + "' fail: " + e.getMessage(), e);
                }
            });

            tx.commit();
        } catch (Throwable e) {
            initedCollections.remove(collectionId);
            throw e;
        }
    }

    @Override
    protected DocumentCollection doNewTempDocumentCollection() {
        String collectionId = generateTempDocumentCollectionId();
        var collection = tempDbms.getOrCreateDatabase(collectionId);
        initCollectionIfNeeded(collectionId, collection);
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

    protected class CypherArgument extends HashMap<String, Object> {

        @Override
        public Object put(String key, Object value) {
            try {
                return super.put(key.startsWith("$") ? key : "$" + key, codec.serialize(value, String.class));
            } catch (IOException e) {
                throw new IllegalStateException("serialize '" + key + "=" + value + "' fail: " + e.getMessage(), e);
            }
        }

        public String addSingle(Object value) {
            String argName = "$" + size();
            put(argName, value);
            return argName;
        }

        public String addList(Collection<?> values) {
            String argName = "$" + size();
            super.put(argName, values.stream().map(v -> {
                try {
                    return codec.serialize(v, String.class);
                } catch (IOException e) {
                    throw new IllegalStateException("serialize value '" + v + "' fail: " + e.getMessage(), e);
                }
            }));
            return argName;
        }
    }
}
