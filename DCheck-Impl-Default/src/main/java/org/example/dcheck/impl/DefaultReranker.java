package org.example.dcheck.impl;

import com.google.gson.Gson;
import lombok.*;
import okhttp3.*;
import org.example.dcheck.api.*;
import org.example.dcheck.spi.ConfigProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Date: 2025/2/26
 * depend on reranker server written by python <a href="https://gitee.com/GiteeHuanyu/DCheck-Impl-Default-Reranker">Reranker Server</a>
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public class DefaultReranker implements Reranker {

    public static final MediaType JSON_TYPE = MediaType.get("application/json");
    @Getter
    @Setter
    @NonNull
    private Gson gson = new Gson();

    private OkHttpClient client;

    protected OkHttpClient buildClient() {
        return new OkHttpClient.Builder()
                .pingInterval(Duration.ofSeconds(7))
                // local run to reduce context shift cost
                .dispatcher(new Dispatcher(new AbstractExecutorService() {
                    @Override
                    public void shutdown() {
                    }

                    @NotNull
                    @Override
                    public List<Runnable> shutdownNow() {
                        return Collections.emptyList();
                    }

                    @Override
                    public boolean isShutdown() {
                        return true;
                    }

                    @Override
                    public boolean isTerminated() {
                        return false;
                    }

                    @Override
                    public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) {
                        return true;
                    }

                    @Override
                    public void execute(@NotNull Runnable command) {
                        command.run();
                    }
                }))
                .build();
    }

    @Override
    public void init() {
        RequestTemplate.init();
        try (var resp = getClient().newCall(RequestTemplate.INIT.getRequest()).execute()) {
            if (resp.body() == null) {
                throw new IOException("response body is null");
            }
            var initResponse = gson.fromJson(new String(resp.body().bytes()), BaseResponse.class);
            if (initResponse.isSuccess()) {
                return;
            }
            throw new IOException(initResponse.getCause());
        } catch (IOException e) {
            throw new IllegalStateException("init reranker server fail:", e);
        }
    }

    private String castToText(Content content) {
        //TODO support other content type
        return content instanceof TextContent ? ((TextContent) content).getText().toString() : "";
    }

    @Data
    protected static class BaseResponse {
        private boolean success;
        private String msg;
        private String cause;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    protected static class RerankResponse extends BaseResponse {
        private float[][] scores;
    }

    @Data
    @AllArgsConstructor
    protected static class RerankRequest {
        private List<String> query;
        private List<List<String>> passages;
    }


    public OkHttpClient getClient() {
        if (client != null) return client;
        synchronized (this) {
            if (client != null) return client;
            client = buildClient();
        }
        return client;
    }

    public void setClient(@NonNull OkHttpClient client) {
        this.client = client;
    }

    @Override
    public ParagraphRelevancyQueryResult rerank(ParagraphRelevancyQueryResult relevancyResult, ParagraphRelevancyQuery query) {
        try (var resp = getClient().newCall(
                RequestTemplate.RERANK.getBuilder()
                        .post(RequestBody.create(gson.toJson(
                                new RerankRequest(
                                        query.getParagraphs().stream().map(this::castToText).collect(Collectors.toList()),
                                        relevancyResult.getRecords().stream()
                                                .map(records -> records.stream()
                                                        .map(ParagraphRelevancyQueryResult.Record::getContent)
                                                        .map(this::castToText)
                                                        .collect(Collectors.toList()))
                                                .collect(Collectors.toList())
                                )
                        ), JSON_TYPE))
                        .build()
        ).execute()) {
            if (resp.body() == null) {
                throw new IOException("response body is null");
            }

            var rerankResponse = gson.fromJson(new String(resp.body().bytes()), RerankResponse.class);
            if (!rerankResponse.isSuccess()) {
                throw new IllegalStateException("rerank fail: " + rerankResponse.getCause());
            }

            var reranked = IntStream.range(0, relevancyResult.getRecords().size())
                    .mapToObj(i -> {
                        var currentQueryEmbeddingResult = relevancyResult.getRecords().get(i);
                        float[] currentQueryScores = rerankResponse.getScores()[i];
                        if (currentQueryScores.length != currentQueryEmbeddingResult.size()) {
                            throw new IllegalStateException("rerank fail: response scores length not match");
                        }
                        return IntStream.range(0, currentQueryEmbeddingResult.size())
                                .mapToObj(j -> currentQueryEmbeddingResult.get(i).withRelevancy(currentQueryScores[j]))
                                .collect(Collectors.toList());
                    }).collect(Collectors.toList());

            return relevancyResult.withRecords(reranked);
        } catch (IOException e) {
            throw new IllegalStateException("rerank fail: " + e.getMessage(), e);
        }
    }

    @Getter
    @RequiredArgsConstructor
    protected enum RequestTemplate {
        INIT(new Request.Builder()
                .method("GET", null)
                .url("http://127.0.0.1:8080/api/v1/init")
                .build()),
        RERANK(new Request.Builder()
                .method("POST", null)
                .url("http://127.0.0.1:8080/api/v1/rerank")
                .build());
        private Request request;
        private Request.Builder builder;

        RequestTemplate(Request request) {
            this.request = request;
        }

        public static void init() {
            for (RequestTemplate ins : values()) {
                ins.updateRequest();
            }
        }

        public void updateRequest() {
            // base config
            request = request.newBuilder().header("User-Agent", "DCheck Java/0.0.x").build();

            String urlStr = ConfigProvider.getInstance().getApiConfig().getProperty(ApiConfig.RERANKING_MODEL_URL);
            if (urlStr == null) return;
            HttpUrl url;
            try {
                url = HttpUrl.get(urlStr);
            } catch (Exception e) {
                throw new IllegalArgumentException("invalid config '" + ApiConfig.RERANKING_MODEL_URL + "=" + urlStr + "'", e);
            }
            request = request.newBuilder().url(request.url().newBuilder().host(url.host()).port(url.port()).build()).build();
            builder = request.newBuilder();
        }
    }
}
