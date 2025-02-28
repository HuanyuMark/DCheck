package org.example.dcheck.impl;

import com.google.gson.Gson;
import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeException;
import dev.failsafe.RetryPolicy;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.commons.io.IOUtils;
import org.example.dcheck.api.*;
import org.example.dcheck.spi.ConfigProvider;
import org.example.dcheck.spi.MapSpi;
import org.springframework.util.StringUtils;
import tech.amikos.chromadb.Client;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.handler.ApiException;
import tech.amikos.chromadb.model.QueryEmbedding;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@Slf4j
public class ChromaParagraphRelevancyEngine implements ParagraphRelevancyEngine {

    public static final List<QueryEmbedding.IncludeEnum> QUERY_PARAGRAPH_INCLUDE = Arrays.asList(QueryEmbedding.IncludeEnum.METADATAS, QueryEmbedding.IncludeEnum.DISTANCES, QueryEmbedding.IncludeEnum.DOCUMENTS);

    private final Map<String, Collection> chromaCollections = new ConcurrentSkipListMap<>();
    private final Map<String, ChromaDocumentCollection> documentCollections = new ConcurrentSkipListMap<>();
    private final Set<TempDocumentCollection> tempDocumentCollections = ConcurrentHashMap.newKeySet();


    private Client client;
    @Getter
    @Setter
    @NonNull
    private EmbeddingFunction embeddingFunction;
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
    @Setter
    @NonNull
    private Gson gson = SerializerSupport.getGson();


    private volatile boolean init;


    public ChromaParagraphRelevancyEngine() {

    }

    @Override
    public void init() {
        if (init) {
            return;
        }
        synchronized (this) {
            if (init) {
                return;
            }

            var embeddingModel = ConfigProvider.getInstance().getApiConfig().getProperty(ApiConfig.EMBEDDING_MODEL_KEY, ApiConfig.DEFAULT_VALUE);
            embeddingFunction = MapSpi.getInstance().getFunc(embeddingModel);

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
                        log.info("Starting chroma connection testing");
                        try {
                            Failsafe.with(policy).run(() -> client.heartbeat());
                            log.info("Finished chroma connection testing");
                        } catch (FailsafeException e) {
                            throw new IllegalStateException("connect to chroma server fail:", e.getCause());
                        }
                    }),
                    // init reranker
                    CompletableFuture.runAsync(() -> {
                        String rerankModel = ConfigProvider.getInstance().getApiConfig().getProperty(ApiConfig.RERANKING_MODEL_KEY);
                        if (rerankModel == null) return;
                        reranker = MapSpi.getInstance().getReranker(rerankModel);
                        log.info("Starting init Reranker '{}'", rerankModel.getClass().getCanonicalName());
                        try {
                            reranker.init();
                            log.info("Finished init Reranker");
                        } catch (Exception e) {
                            throw new IllegalStateException("init reranker fail: " + e.getMessage(), e);
                        }
                    })
            ).join();

            // Backup plan: delete all temp docs
            Runtime.getRuntime().addShutdownHook(new Thread() {
                {
                    setName(ChromaParagraphRelevancyEngine.this.getClass().getName() + "::shutdownHook");
                }

                @Override
                public void run() {
                    if (tempDocumentCollections.isEmpty()) {
                        log.info("[TempDocumentCollection Leak Detection]: all collections has been closed, good job!");
                        return;
                    }
                    log.warn("[TempDocumentCollection Leak Detection]: remember closing the collection after using (try-with-resources statement is best practice)");
                    for (TempDocumentCollection collection : tempDocumentCollections) {
                        try {
                            collection.close();
                        } catch (Exception e) {
                            log.warn("close leaked collection fail: {}", e.getMessage(), e);
                        }
                    }
                }
            });
            init = true;
        }
    }

    @Override
    public ParagraphRelevancyQueryResult queryParagraph(ParagraphRelevancyQuery query) {
        init();
        var documentCollection = getOrCreateDocumentCollection(query.getCollectionId());
        Collection collection = documentCollection.getCollection();
        Collection.QueryResponse response;
        try {
            // 1. query embedding
            response = Failsafe.with(collectionAccessPolicy)
                    .get(() -> collection.query(
                            query.getParagraphs().stream().map(p -> {
                                if (p instanceof InMemoryTextContent)
                                    return ((InMemoryTextContent) p).getText().toString();
                                if (p instanceof TextContent) {
                                    try {
                                        return new String(IOUtils.toByteArray(p.getInputStream()));
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                // TODO support other types
                                return "";
                            }).collect(Collectors.toList()),
                            query.getTopK(),
                            // exclude self
                            Collections.singletonMap("documentId", Collections.singletonMap("$ne", query.getCollectionId())),
                            null,
                            QUERY_PARAGRAPH_INCLUDE
                    ));
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
                        // TODO refer to metadata.type, we can reconstruct multiple paragraph type
                        // now only support text type
                        var document = queryResultDocument.get(j);
                        @SuppressWarnings("unchecked")
                        var metadata = (Map<String, String>) ((Object) queryResultMetadata.get(j));
                        var score = queryResultScore.get(j);
                        return ParagraphRelevancyQueryResult.Record.builder()
                                .paragraph(TextParagraph.mapBuilder()
                                        .fromFlat(metadata, gson::fromJson)
                                        .collection(documentCollection)
                                        .content(() -> new InMemoryTextContent(document))
                                        .build())
                                .relevancy(score)
                                .build();
                    }).collect(Collectors.toList());
                }).collect(Collectors.toList());

        var queryEmbeddingRes = builder.records(result).build();

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
                Failsafe.with(collectionAccessPolicy)
                        .run(() -> collection.add(
                                null,
                                textParagraphs.stream().map(ParagraphRelevancyCreation.Record::getMetadata).map(m -> m.toFlatMap(gson::toJson)).collect(Collectors.toList()),
                                textParagraphs.stream().map(ParagraphRelevancyCreation.Record::getParagraph).map(p -> {
                                    if (p.getContent() instanceof InMemoryTextContent)
                                        return ((InMemoryTextContent) p.getContent()).getText().toString();
                                    if (p.getContent() instanceof TextContent) {
                                        try {
                                            return new String(IOUtils.toByteArray(p.getContent().getInputStream()));
                                        } catch (IOException e) {
                                            throw new IllegalStateException("read text paragraph fail:", e);
                                        }
                                    }
                                    throw new UnsupportedOperationException();
                                }).collect(Collectors.toList()),
                                // 这里的id是否需要预先生成？
                                textParagraphs.stream().map(e -> UUID.randomUUID().toString()).collect(Collectors.toList())
                        ));
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

    @Override
    public ChromaDocumentCollection getOrCreateDocumentCollection(String collectionId) {
        init();
        return documentCollections.computeIfAbsent(collectionId, key -> {
            Collection collection = getCollection(key);
            return new ChromaDocumentCollection(collection, this);
        });
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

    @Override
    public TempDocumentCollection newTempDocumentCollection() {
        init();
        var co = new TempDocumentCollectionAdaptor(getOrCreateDocumentCollection(UUID.randomUUID().toString())) {
            @Override
            public void drop() {
                super.drop();
                tempDocumentCollections.remove(this);
            }
        };
        tempDocumentCollections.add(co);
        return co;
    }

    protected Collection getCollection(String collectionId) {
        return chromaCollections.computeIfAbsent(collectionId, (key) -> {
            try {
                return Failsafe.with(collectionAccessPolicy)
                        .get(() -> client.createCollection(
                                collectionId,
                                new HashMap<String, String>() {{
                                    put("hnsw:space", "cosine");
                                    put("createTime", String.valueOf(System.currentTimeMillis()));
                                }},
                                Boolean.TRUE,
                                Objects.requireNonNull(embeddingFunction)));
            } catch (FailsafeException e) {
                throw new IllegalStateException("access chroma collection fail:", e.getCause());
            }
        });
    }
}
