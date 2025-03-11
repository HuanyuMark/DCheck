package org.example.dcheck.impl;

import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeException;
import dev.failsafe.RetryPolicy;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.example.dcheck.api.*;
import org.example.dcheck.api.embedding.Embedding;
import org.example.dcheck.api.embedding.EmbeddingFunction;
import org.example.dcheck.common.util.CollectionUtils;
import org.example.dcheck.common.util.ContentConvert;
import org.example.dcheck.spi.CodecProvider;
import org.example.dcheck.spi.ConfigProvider;
import org.example.dcheck.spi.EmbeddingFuncMapProvider;
import org.example.dcheck.spi.RerankerMapProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;
import tech.amikos.chromadb.ChromaCollection;
import tech.amikos.chromadb.Client;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.GetEmbeddingInclude;
import tech.amikos.chromadb.handler.ApiException;
import tech.amikos.chromadb.model.AnyOfGetEmbeddingIncludeItems;
import tech.amikos.chromadb.model.GetEmbedding;
import tech.amikos.chromadb.model.QueryEmbedding;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@Slf4j
@SuppressWarnings("unused")
public class ChromaParagraphRelevancyEngine extends AbstractParagraphRelevancyEngine implements ParagraphRelevancyEngine {

    public static final List<QueryEmbedding.IncludeEnum> QUERY_PARAGRAPH_INCLUDE = Arrays.asList(QueryEmbedding.IncludeEnum.METADATAS, QueryEmbedding.IncludeEnum.DISTANCES, QueryEmbedding.IncludeEnum.DOCUMENTS);
    public static final List<AnyOfGetEmbeddingIncludeItems> GET_PARAGRAPH_INCLUDE = Arrays.asList(GetEmbeddingInclude.metadatas, GetEmbeddingInclude.documents);
    protected static final String TEMP_COLLECTION_PREFIX = "tmp9843975u";
    private static final int CHUNK_SIZE = 10;

//    static {
//        try {
//            // use the name with unescaped char to avoid name conflict
//            TEMP_COLLECTION_PREFIX = URLEncoder.encode("tm$p9843975uy6hn3w$x2zc$p8o435a$s5poq", StandardCharsets.UTF_8.name());
//        } catch (UnsupportedEncodingException e) {
//            throw new RuntimeException(e);
//        }
//    }

    private final Map<String, ChromaCollection> chromaCollections = new ConcurrentSkipListMap<>();
    private final Map<String, EngineAdaptedDocumentCollection> documentCollections = new ConcurrentSkipListMap<>();
    protected static final String EMBEDDING_FUNC_KEY = "embedding_function";
    @Getter
    private EmbeddingFunction embeddingFunction;

    private String embeddingFuncDetailsKey;

    public void setEmbeddingFunction(@NonNull EmbeddingFunction embeddingFunction) {
        this.embeddingFunction = embeddingFunction;
        try {
            embeddingFuncDetailsKey = codec.serialize(embeddingFunction.getDetails(), String.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("serialize embedding function details '" + embeddingFunction.getDetails() + "' fail: " + e.getMessage(), e);
        }
    }

    private final RetryPolicy<Object> collectionAccessPolicy = RetryPolicy.builder()
            .handle(ApiException.class)
            .withMaxRetries(3)
            // 初始等待1s，最多30s,每次重试时间以2倍增长
            .withBackoff(Duration.ofSeconds(1), Duration.ofSeconds(5), 1.5)
            .build();

    @Getter
    @Setter
    private Reranker reranker = Reranker.NOP;
    @Getter
    private Codec codec;

    public void setCodec(@NonNull Codec codec) {
        this.codec = codec;
    }

    public ChromaParagraphRelevancyEngine() {

    }

    @Override
    protected void doInit() {
        if (codec == null) {
            codec = CodecProvider.getInstance()
                    .getCodecs()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("manual set codec before init(), otherwise list " + Codec.class + " provider in classpath"));
        }

        var embeddingModel = ConfigProvider.getInstance().getApiConfig().getProperty(ApiConfig.EMBEDDING_MODEL_KEY, ApiConfig.DEFAULT_VALUE);
        setEmbeddingFunction(EmbeddingFuncMapProvider.getInstance().getFunc(embeddingModel));

        var url = ConfigProvider.getInstance().getApiConfig().getProperty(ApiConfig.DB_VECTOR_URL);
        if (!StringUtils.hasText(url)) {
            throw new IllegalStateException("invalid config '" + ApiConfig.DB_VECTOR_URL + "=" + url + "'");
        }

        CompletableFuture.allOf(
                // init embedding function
                CompletableFuture.runAsync(() -> {
                    try {
                        log.info("Starting init Embedding Function '{}'", embeddingFunction.getClass().getCanonicalName());
                        embeddingFunction.init();
                        log.info("Finished init Embedding Function");
                    } catch (Exception e) {
                        throw new IllegalStateException("init embedding function fail:", e);
                    }
                }),

                // init chroma client
                CompletableFuture.runAsync(() -> {
                    client = new Client(url);
                    var policy = RetryPolicy.builder()
                            .handle(ApiException.class)
                            .withMaxRetries(3)
                            // 初始等待1s，最多30s,每次重试时间以2倍增长
                            .withBackoff(Duration.ofSeconds(1), Duration.ofSeconds(30), 2)
                            .build();

                    // make sure the connection is ok
                    log.info("Starting chroma connection testing");
                    try {
                        Failsafe.with(policy).run(() -> client.heartbeat());
                        log.info("Finished chroma connection testing");
                    } catch (FailsafeException e) {
                        throw new IllegalStateException("connect to chroma server fail: " + e.getMessage(), e.getCause());
                    }

                    // Server End: clean temp document collection
                    try {
                        for (Collection collection : Failsafe.with(policy).get(() -> client.listCollections())) {
                            if (isTempDocumentCollection(collection.getName())) {
                                Failsafe.with(policy).run(() -> client.deleteCollection(collection.getName()));
                            }
                        }
                    } catch (FailsafeException e) {
                        throw new IllegalStateException("clean temp document collection fail: " + e.getMessage(), e.getCause());
                    }
                }),

                // init reranker
                CompletableFuture.runAsync(() -> {
                    String rerankModel = ConfigProvider.getInstance().getApiConfig().getProperty(ApiConfig.RERANKING_MODEL_KEY);
                    if (rerankModel == null) return;
                    reranker = RerankerMapProvider.getInstance().getReranker(rerankModel);
                    log.info("Starting init Reranker '{}'", rerankModel.getClass().getCanonicalName());
                    try {
                        reranker.init();
                        log.info("Finished init Reranker");
                    } catch (Exception e) {
                        throw new IllegalStateException("init reranker fail: " + e.getMessage(), e);
                    }
                })
        ).join();
    }

    private static final List<AnyOfGetEmbeddingIncludeItems> GET_EMBEDDING_INCLUDES = Collections.singletonList(GetEmbeddingInclude.embeddings);

    @Override
    @SuppressWarnings("unchecked")
    public ParagraphRelevancyQueryResult queryParagraph(ParagraphRelevancyQuery query) {
        init();
        var documentCollection = getOrCreateDocumentCollection(query.getCollectionId());
        ChromaCollection collection = getCollection(documentCollection.getId());
        Collection.QueryResponse response;
        var req = new QueryEmbedding();
        if (query.getParagraphs() == null) {
            List<List<Float>> embeddings;
            try {
                embeddings = Failsafe.with(collectionAccessPolicy)
                        .get(() -> collection.get(new GetEmbedding()
                                .where(ChromaDSLFactory.where(MetadataMatchCondition.builder().eq("documentId", query.getDocumentId()).build()))
                                .include(GET_EMBEDDING_INCLUDES)
                        ).getEmbeddings());
                req.setQueryEmbeddings((List<Object>) ((Object) embeddings));
            } catch (FailsafeException e) {
                throw new IllegalStateException("query document embeddings fail: " + e.getMessage(), e);
            }
        } else {
            try {
                req.setQueryEmbeddings(embeddingFunction.embedDocuments(query.getParagraphs().stream().map(ContentConvert::castToText).collect(Collectors.toList())).stream().map(Embedding::asArray).collect(Collectors.toList()));
            } catch (Exception e) {
                throw new IllegalStateException("calculate paragraph embeddings fail: " + e.getMessage(), e);
            }
        }
        req.setNResults(query.getTopK());
        req.setWhere(ChromaDSLFactory.where(MetadataMatchCondition.builder().ne("documentId", query.getDocumentId()).build()));
        req.setInclude(QUERY_PARAGRAPH_INCLUDE);
        try {
            // 1. query KNN by embedding
            response = Failsafe.with(collectionAccessPolicy)
                    .get(() -> collection.query(req));
        } catch (FailsafeException e) {
            throw new IllegalStateException("query paragraph fail: " + e.getCause().getMessage(), e.getCause());
        }

        var builder = ParagraphRelevancyQueryResult.builder();
        var result = IntStream.range(0, response.getDocuments().size())
                .mapToObj(i -> {
                    var queryResultDocument = response.getDocuments().get(i);
                    var queryResultMetadata = response.getMetadatas().get(i);
                    var queryResultScore = response.getDistances().get(i);
                    return IntStream.range(0, queryResultDocument.size()).mapToObj(j -> {
                        var document = queryResultDocument.get(j);
                        @SuppressWarnings("unchecked")
                        var metadata = ((Map<String, String>) ((Object) queryResultMetadata.get(j)))
                                .entrySet().stream().map(e -> {
                                    try {
                                        return new AbstractMap.SimpleEntry<>(e.getKey(), codec.deserialize(e.getValue(), Object.class));
                                    } catch (IOException ex) {
                                        throw new IllegalStateException("deserialize metadata '" + e.getKey() + "=" + e.getValue() + "' fail: " + ex.getMessage(), ex);
                                    }
                                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                        var score = queryResultScore.get(j);
                        ParagraphMetadata metadataObj;
                        try {
                            metadataObj = codec.convertTo(metadata, ParagraphMetadata.class);
                        } catch (IOException e) {
                            throw new IllegalArgumentException("parse metadata fail: " + e.getMessage(), e);
                        }

                        return ParagraphRelevancyQueryResult.Record.builder()
                                .paragraph(metadataObj.getParagraphType().createParagraph(document, metadataObj))
                                .relevancy(score)
                                .build();
                    }).collect(Collectors.toList());
                }).collect(Collectors.toList());

        ParagraphRelevancyQueryResult queryEmbeddingRes = builder.records(result).build();

        //2. rerank
        return reranker.rerank(queryEmbeddingRes, query);
    }

    @Override
    public void addParagraph(ParagraphRelevancyCreation creation) {
        init();
        var collection = getCollection(creation.getCollectionId());
        var batch = creation.getBatch().stream().collect(Collectors.groupingBy(ParagraphRelevancyCreation.Record::getParagraphType));
        var textParagraphs = batch.get(BuiltinParagraphType.TEXT);
        if (textParagraphs != null) {
            try {
                var partition = CollectionUtils.partition(textParagraphs, CHUNK_SIZE);
                for (int i = 0; i < partition.size(); i++) {
                    var chunk = partition.get(i);
                    Failsafe.with(collectionAccessPolicy)
                            .run(() -> collection.add(
                                    null,
                                    chunk.stream()
                                            .map(ParagraphRelevancyCreation.Record::getMetadata)
                                            .map(m -> m.toFlatMap(form -> {
                                                try {
                                                    return codec.serialize(form, String.class);
                                                } catch (IOException e) {
                                                    throw new IllegalStateException("stringfy obj '" + form + "' to json fail: " + e.getMessage(), e);
                                                }
                                            }))
                                            .collect(Collectors.toList()),
                                    chunk.stream()
                                            .map(ParagraphRelevancyCreation.Record::getParagraph)
                                            .map(p -> ContentConvert.castToText(p.getContent())).collect(Collectors.toList()),
                                    // 这里的id是否需要预先生成？
                                    chunk.stream().map(e -> UUID.randomUUID().toString()).collect(Collectors.toList())
                            ));
                    log.debug("add paragraph progress:  {}/{}", i + 1, partition.size());
                }
            } catch (FailsafeException e) {
                throw new IllegalStateException("add paragraph fail:", e.getCause());
            }
        }

        //TODO handle other types
    }

    @Override
    public void removeDocument(DocumentDelete delete) {
        init();
        var collection = getCollection(delete.getCollectionId());
        try {
            var where = ChromaDSLFactory.where(delete.getMetadataMatchCondition());
            Failsafe.with(collectionAccessPolicy)
                    .run(() -> collection.deleteWhere(where));
        } catch (FailsafeException e) {
            throw new IllegalStateException("delete paragraph fail:", e.getCause());
        }
    }

    /**
     * low performance. Facade Batch
     * chroma doc: batch op 'get' is nonexistent
     */
    @Override
    public List<Boolean> hasDocument(DocumentIdQuery query) {
        var collection = getCollection(query.getCollectionId());
        try {
            return query.getDocumentIds().stream().map(id -> {
                var req = new GetEmbedding();
                req.limit(1);
                try {
                    req.where(ChromaDSLFactory.where(MetadataMatchCondition.builder()
                            .eq("documentId", codec.serialize(id, String.class))
                            .build()));
                } catch (IOException e) {
                    throw new IllegalStateException("serialize 'documentId=" + id + "' fail: " + e.getMessage(), e);
                }
                req.include(Collections.emptyList());
                return Failsafe.with(collectionAccessPolicy)
                        .get(() -> collection.get(req))
                        .getIds().stream().findFirst().map(v -> Boolean.TRUE).orElse(Boolean.FALSE);
            }).collect(Collectors.toList());
        } catch (FailsafeException e) {
            throw new IllegalStateException("query has document fail: " + e.getMessage(), e.getCause());
        }
    }

    @Override
    protected DocumentCollection doNewTempDocumentCollection() {
        return getEngineAdaptedDocumentCollection(generateTempDocumentCollectionId());
    }

    @Override
    public List<Paragraph> getParagraphs(ParagraphGet query) {
        var collection = getCollection(query.getCollectionId());
        var req = new GetEmbedding();
        if (query.getMaxCount() != null) {
            req.limit(query.getMaxCount());
        }

        if (query.getCondition() != null) {
            req.where(ChromaDSLFactory.where(query.getCondition(), e -> {
                try {
                    return codec.serialize(e.getValue(), String.class);
                } catch (IOException ex) {
                    throw new IllegalStateException("serialize '" + e.getKey() + "=" + e.getValue() + "' fail: " + ex.getMessage(), ex);
                }
            })).include(GET_PARAGRAPH_INCLUDE);
        }

        try {
            var getResult = Failsafe.with(collectionAccessPolicy)
                    .get(() -> collection.get(req));
            return IntStream.range(0, getResult.getIds().size())
                    .mapToObj(i -> {
                        Map<String, Object> flatMetadata = getResult.getMetadatas().get(i);
                        Map<String, Object> objMetadata = flatMetadata.entrySet().stream()
                                .map(e -> {
                                    try {
                                        return new AbstractMap.SimpleEntry<>(e.getKey(), codec.deserialize(e.getValue(), Object.class));
                                    } catch (IOException ex) {
                                        throw new IllegalStateException("deserialize metadata '" + e.getKey() + "=" + e.getValue() + "' fail: " + ex.getMessage(), ex);
                                    }
                                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                        ParagraphMetadata metadata;
                        try {
                            metadata = codec.convertTo(objMetadata, ParagraphMetadata.class);
                        } catch (IOException e) {
                            throw new IllegalStateException("convert metadata to ParagraphMetadata fail: " + e.getMessage(), e);
                        }

                        var paragraphClass = metadata.getParagraphType().getParagraphClass();

                        String document = getResult.getDocuments().get(i);

                        return metadata.getParagraphType().createParagraph(document, metadata);
                    }).collect(Collectors.toList());
        } catch (FailsafeException e) {
            throw new IllegalStateException("getParagraphs fail: " + e.getMessage(), e.getCause());
        }
    }

    @Override
    public EngineAdaptedDocumentCollection getOrCreateDocumentCollection(String collectionId) {
        if (collectionId.startsWith(TEMP_COLLECTION_PREFIX)) {
            throw new IllegalArgumentException("invalid collectionId '" + collectionId + "'");
        }
        return getEngineAdaptedDocumentCollection(collectionId);
    }

    @NotNull
    private EngineAdaptedDocumentCollection getEngineAdaptedDocumentCollection(String collectionId) {
        init();
        return documentCollections.computeIfAbsent(collectionId, key -> new EngineAdaptedDocumentCollection(getCollection(key).getName(), this));
    }

    @Override
    public void removeDocumentCollection(String collectionId) {
        init();
        try {
            Failsafe.with(collectionAccessPolicy)
                    .run(() -> client.deleteCollection(collectionId));
        } catch (FailsafeException e) {
            throw new IllegalStateException("delete chroma collection fail:", e.getCause());
        }
        chromaCollections.remove(collectionId);
        documentCollections.remove(collectionId);
    }

    protected static final String EMBEDDING_FUNC_DETAILS_KEY = "__$$_embedding_func_details_$$__";
    @Getter
    private Client client;

    protected ChromaCollection getCollection(String collectionId) {
        var res = chromaCollections.computeIfAbsent(collectionId, (key) -> {
            try {
                return Failsafe.with(collectionAccessPolicy)
                        .get(() -> {
                            Map<String, String> metadata = new HashMap<>();
                            metadata.put("hnsw:space", "cosine");
                            metadata.put("createTime", String.valueOf(System.currentTimeMillis()));
                            metadata.put(EMBEDDING_FUNC_KEY, embeddingFunction.getName());
                            metadata.put(EMBEDDING_FUNC_DETAILS_KEY, embeddingFuncDetailsKey);
                            return new ChromaCollection(client.createCollection(
                                    collectionId,
                                    metadata,
                                    Boolean.TRUE,
                                    ChromaEmbeddingFunctionWrapper.wrap(embeddingFunction)));
                        });
            } catch (FailsafeException e) {
                throw new IllegalStateException("access chroma collection fail:", e.getCause());
            }
        });
        if (!Objects.equals(res.getMetadata().get(EMBEDDING_FUNC_KEY), embeddingFunction.getName()) ||
                !Objects.equals(res.getMetadata().get(EMBEDDING_FUNC_DETAILS_KEY), embeddingFuncDetailsKey)) {
            throw new IllegalStateException("chroma collection embedding function or embedding function state not match");
        }
        return res;
    }

    @Override
    protected String generateTempDocumentCollectionId() {
        return TEMP_COLLECTION_PREFIX + "_" + (long) (System.currentTimeMillis() / Math.random());
    }

    protected boolean isTempDocumentCollection(String collectionId) {
        return collectionId.startsWith(TEMP_COLLECTION_PREFIX);
    }

    @Override
    public void close() {

    }
}
