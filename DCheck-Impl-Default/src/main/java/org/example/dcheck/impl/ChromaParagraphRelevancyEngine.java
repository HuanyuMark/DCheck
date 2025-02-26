package org.example.dcheck.impl;

import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeException;
import dev.failsafe.RetryPolicy;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
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
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
public class ChromaParagraphRelevancyEngine implements ParagraphRelevancyEngine {

    public static final List<QueryEmbedding.IncludeEnum> QUERY_PARAGRAPH_INCLUDE = Arrays.asList(QueryEmbedding.IncludeEnum.METADATAS, QueryEmbedding.IncludeEnum.DISTANCES, QueryEmbedding.IncludeEnum.DOCUMENTS);
    private final Map<String, Collection> cachedCollections = new ConcurrentSkipListMap<>();
    private final RetryPolicy<Object> collectionAccessPolicy = RetryPolicy.builder()
            .handle(ApiException.class)
            .withMaxRetries(3)
            // 初始等待1s，最多30s,每次重试时间以2倍增长
            .withBackoff(Duration.ofSeconds(1), Duration.ofSeconds(5), 1.5)
            .build();
    private Client client;
    @Getter
    @Setter
    @NonNull
    private EmbeddingFunction embeddingFunction;

    @Getter
    @Setter
    private Reranker reranker;

    public ChromaParagraphRelevancyEngine() {

    }

    @Override
    public void init() {
        var url = ConfigProvider.getInstance().getApiConfig().getProperty(ApiConfig.DB_VECTOR_URL);
        if (!StringUtils.hasText(url)) {
            throw new IllegalStateException("invalid config '" + ApiConfig.DB_VECTOR_URL + "=" + url + "'");
        }
        CompletableFuture.allOf(
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
    }

    @Override
    public ParagraphRelevancyQueryResult queryParagraph(ParagraphRelevancyQuery query) {
        Collection collection = getCollection(query.getCollectionId());
        try {
            return Failsafe.with(collectionAccessPolicy)
                    .get(() -> {
                        var response = collection.query(
                                query.getParagraphs().stream().map(p -> {
                                    if (p instanceof TextContent) return ((TextContent) p).getText().toString();
                                    // TODO support other types
                                    return "";
                                }).collect(Collectors.toList()),
                                query.getTopK(),
                                null,
                                null,
                                QUERY_PARAGRAPH_INCLUDE
                        );
                        var builder = ParagraphRelevancyQueryResult.builder();
                        IntStream.range(0, response.getDocuments().size())
                                .mapToObj(i -> {
                                    //TODO 构造结果
                                    var metadata = response.getMetadatas().get(0).get(i);
//                                    metadata.get()
                                    return ParagraphRelevancyQueryResult.Record.builder()
                                            .paragraph(TextParagraph.builder().build());
                                });
                        return builder.build();
//                            response.getDocuments()
                    });
        } catch (FailsafeException e) {
            throw new IllegalStateException("query paragraph fail: " + e.getCause().getMessage(), e.getCause());
        }
//        throw new UnsupportedOperationException();
    }

    @Override
    public void addParagraph(ParagraphRelevancyCreation creation) {
        var collection = getCollection(creation.getCollectionId());
        var batch = creation.getBatch().stream().collect(Collectors.groupingBy(ParagraphRelevancyCreation.Record::getParagraphType));
        try {
            Failsafe.with(collectionAccessPolicy)
                    .run(() -> {
                        var textParagraphs = batch.get(BuiltinParagraphType.TEXT);
                        if (textParagraphs != null) {
                            collection.add(
                                    null,
                                    textParagraphs.stream().map(ParagraphRelevancyCreation.Record::getMetadata).collect(Collectors.toList()),
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
        var collection = getCollection(delete.getCollectionId());
        try {
            var metadataMatch = delete.getMetadataMatchCondition().getEqs().entrySet().stream()
                    .map(e -> new AbstractMap.SimpleEntry<String, Object>(e.getKey(), Collections.singletonMap("$eq", e.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Failsafe.with(collectionAccessPolicy)
                    .run(() -> collection.deleteWhere(metadataMatch));
        } catch (FailsafeException e) {
            throw new IllegalStateException("delete paragraph fail:", e.getCause());
        }
    }

    @Override
    public DocumentCollection getOrCreateDocumentCollection(String id) {
        return null;
    }

    @Override
    public TempDocumentCollection newTempDocumentCollection() {
        return null;
    }

    protected Collection getCollection(String collectionId) {
        return cachedCollections.computeIfAbsent(collectionId, (key) -> {
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

    void removeCollection(String collectionId) {
        try {
            Failsafe.with(collectionAccessPolicy)
                    .run(() -> {
                        client.deleteCollection(collectionId);
                        cachedCollections.remove(collectionId);
                    });
        } catch (FailsafeException e) {
            throw new IllegalStateException("delete chroma collection fail:", e.getCause());
        }
    }

}
