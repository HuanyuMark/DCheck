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
    private Gson gson;
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
                            embeddingFunction.init();
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
                        try {
                            Failsafe.with(policy).run(() -> client.heartbeat());
                        } catch (FailsafeException e) {
                            throw new IllegalStateException("connect to chroma server fail:", e.getCause());
                        }
                    }),
                    // init reranker
                    CompletableFuture.runAsync(() -> {
                        String rerankModel = ConfigProvider.getInstance().getApiConfig().getProperty(ApiConfig.RERANKING_MODEL_KEY);
                        if (rerankModel == null) return;
                        reranker = MapSpi.getInstance().getReranker(rerankModel);
                        try {
                            reranker.init();
                        } catch (Exception e) {
                            throw new IllegalStateException("init reranker fail: " + e.getMessage(), e);
                        }
                    })
            ).join();

            // Backup plan: delete all temp docs
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (tempDocumentCollections.isEmpty()) {
                    log.info("[TempDocumentCollection Leak Detection]: all collections has been closed, good job!");
                    return;
                }
                log.warn("[TempDocumentCollection Leak Detection]: remember closing the collection after using (try-with-resources statement is best practice)");
                tempDocumentCollections.forEach(TempDocumentCollection::close);
            }));
            init = true;
        }
    }

    @Override
    public ParagraphRelevancyQueryResult queryParagraph(ParagraphRelevancyQuery query) {
        init();
        var documentCollection = getOrCreateDocumentCollection(query.getCollectionId());
        Collection collection = documentCollection.getCollection();

        try {
            // 1. query embedding
            var queryEmbeddingRes = Failsafe.with(collectionAccessPolicy)
                    .get(() -> {
                        var response = collection.query(
                                query.getParagraphs().stream().map(p -> {
                                    if (p instanceof TextContent) return ((TextContent) p).getText().toString();
                                    // TODO support other types
                                    return "";
                                }).collect(Collectors.toList()),
                                query.getTopK(),
                                // exclude self
                                Collections.singletonMap("documentId", Collections.singletonMap("$ne", query.getCollectionId())),
                                null,
                                QUERY_PARAGRAPH_INCLUDE
                        );
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
                                        var metadata = queryResultMetadata.get(j);
                                        var score = queryResultScore.get(j);
                                        return ParagraphRelevancyQueryResult.Record.builder()
                                                .paragraph(TextParagraph.builder()
                                                        .collection(documentCollection)
                                                        .content(() -> new TextContent(document))
                                                        .location(gson.fromJson(((String) metadata.get("location")), TextParagraphLocation.class))
                                                        .documentId((String) metadata.get("documentId"))
                                                        .build())
                                                .relevancy(score)
                                                .build();
                                    }).collect(Collectors.toList());
                                }).collect(Collectors.toList());
                        return builder.records(result).build();
                    });

            //2. rerank
            return reranker.rerank(queryEmbeddingRes, query);
        } catch (FailsafeException e) {
            throw new IllegalStateException("query paragraph fail: " + e.getCause().getMessage(), e.getCause());
        }
//        throw new UnsupportedOperationException();
    }

    @Override
    public void addParagraph(ParagraphRelevancyCreation creation) {
        init();
        var collection = getCollection(creation.getCollectionId());
        var batch = creation.getBatch().stream().collect(Collectors.groupingBy(ParagraphRelevancyCreation.Record::getParagraphType));
        try {
            Failsafe.with(collectionAccessPolicy)
                    .run(() -> {
                        var textParagraphs = batch.get(BuiltinParagraphType.TEXT);
                        if (textParagraphs != null) {
                            collection.add(
                                    null,
                                    textParagraphs.stream().map(ParagraphRelevancyCreation.Record::getMetadata).map(m -> m.toFlatMap(gson::toJson)).collect(Collectors.toList()),
                                    textParagraphs.stream().map(ParagraphRelevancyCreation.Record::getParagraph).map(p -> {
                                        if (p.getContent() instanceof TextContent)
                                            return ((TextContent) p.getContent()).getText().toString();
                                        try {
                                            return new String(IOUtils.toByteArray(p.getContent().getInputStream()));
                                        } catch (IOException e) {
                                            throw new IllegalStateException("read text paragraph fail:", e);
                                        }
                                    }).collect(Collectors.toList()),
                                    // 这里的id是否需要预先生成？
                                    textParagraphs.stream().map(e -> UUID.randomUUID().toString()).collect(Collectors.toList())
                            );
                        }
                        //TODO handle other types
                    });
        } catch (FailsafeException e) {
            throw new IllegalStateException("add paragraph fail:", e.getCause());
        }
    }

    @Override
    public void removeDocument(DocumentDelete delete) {
        init();
        var collection = getCollection(delete.getCollectionId());
        try {
            var condition = delete.getMetadataMatchCondition();
            var eqs = condition.getEqs().entrySet().stream()
                    .map(e -> new AbstractMap.SimpleEntry<String, Object>(e.getKey(), Collections.singletonMap("$eq", e.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            var ins = condition.getIns().entrySet().stream()
                    .map(e -> new AbstractMap.SimpleEntry<String, Object>(e.getKey(), Collections.singletonMap("$in", e.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            var where = new HashMap<String, Object>();
            where.putAll(eqs);
            where.putAll(ins);

            // check if eqs and ins has same keys (field name)
            if (where.size() != eqs.size() + ins.size()) {
                Set<String> bigSet;
                Set<String> smallSet;
                if (eqs.size() > ins.size()) {
                    bigSet = new HashSet<>(eqs.keySet());
                    smallSet = ins.keySet();
                } else {
                    bigSet = new HashSet<>(ins.keySet());
                    smallSet = eqs.keySet();
                }
                bigSet.retainAll(smallSet);
                throw new IllegalArgumentException("field cannot apply $eq and $in statements both: " + bigSet);
            }

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
                    .run(() -> {
                        client.deleteCollection(collectionId);
                        chromaCollections.remove(collectionId);
                        documentCollections.remove(collectionId);
                    });
        } catch (FailsafeException e) {
            throw new IllegalStateException("delete chroma collection fail:", e.getCause());
        }
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
