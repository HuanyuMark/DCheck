package org.example.dcheck.impl.embedding.remote;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.example.dcheck.api.ApiConfig;
import org.example.dcheck.api.Codec;
import org.example.dcheck.api.DCheckComponent;
import org.example.dcheck.api.IEventEmitter;
import org.example.dcheck.api.embedding.Embedding;
import org.example.dcheck.api.embedding.EmbeddingFunction;
import org.example.dcheck.common.util.CollectionUtils;
import org.example.dcheck.spi.CodecProvider;
import org.example.dcheck.spi.ConfigProvider;
import org.example.dcheck.spi.DuplicateCheckingProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import static org.example.dcheck.impl.embedding.remote.ConfigPropertyKey.API_KEY_CONFIG;
import static org.example.dcheck.impl.embedding.remote.ConfigPropertyKey.DIMENSION_CONFIG;

/**
 * Date: 2025/3/8
 *
 * @author 三石而立Sunsy
 */
@Slf4j
@SuppressWarnings("unused")
public class BigModelEmbeddingFunction implements EmbeddingFunction {
    private static final String DEFAULT_MODEL_NAME = "embedding-3";
    private static final String DEFAULT_BASE_API = "https://open.bigmodel.cn/api/paas/v4/embeddings";
    protected static final int maxInputLength = 5;
    @Getter
    private final String modelName;
    @Getter
    private final String baseUrl;
    private final Codec codec;
    private final Map<String, Object> details;
    @Getter
    private Integer dimension;
    private static final int DEFAULT_REQUEST_MAX_TOKEN = 3500;

    private Request embeddingRequestTemplate;
    @Getter
    private OkHttpClient client;
    @Getter
    private int requestMaxToken;

    private volatile boolean initialized;
    private ToIntFunction<String> tokenizer;

    {
        codec = CodecProvider.getInstance().getCodecs().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No codec provider found"));
    }

    @Override
    public Map<String, Object> getDetails() {
        init();
        return details;
    }

    public BigModelEmbeddingFunction(String baseUrl, String modelName) {
        this.baseUrl = baseUrl == null ? DEFAULT_BASE_API : baseUrl;
        this.modelName = modelName == null ? DEFAULT_MODEL_NAME : modelName;
        var details = new HashMap<String, Object>();
        details.put("baseUrl", getBaseUrl());
        details.put("modelName", getModelName());
        details.put("dimension", getDimension());
        this.details = Collections.unmodifiableMap(details);
    }

    public void setClient(@NonNull OkHttpClient client) {
        this.client = client;
    }

    @Nullable
    private Object tokenizerToInject;

    @Override
    public void init() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            if (getClient() == null) {
                setClient(OkHttpClientFactory.getInstance().create());
            }

            HttpUrl url = HttpUrl.parse(this.baseUrl);
            if (url == null) {
                throw new IllegalArgumentException("invalid base url '" + baseUrl + "'");
            }
            ApiConfig apiConfig = ConfigProvider.getInstance().getApiConfig();
            String apiKey = apiConfig.getProperty(API_KEY_CONFIG);
            if (apiKey == null) {
                throw new IllegalArgumentException("missing required config '" + API_KEY_CONFIG + "'");
            }
            var requestHeaders = Headers.of(new HashMap<String, String>() {{
                put("Accept", "application/json");
                put("Content-Type", "application/json");
                put("User-Agent", Constant.HTTP_USER_AGENT);
                put("Authorization", "Bearer " + apiKey);
            }});

            embeddingRequestTemplate = new Request.Builder()
                    .url(url)
                    .headers(requestHeaders)
                    .build();

            String dimensionValueStr = apiConfig.getProperty(DIMENSION_CONFIG);
            if (dimensionValueStr != null) {
                try {
                    dimension = Integer.parseInt(dimensionValueStr);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("invalid config '" + DIMENSION_CONFIG + "=" + dimensionValueStr + "': " + e.getMessage(), e);
                }
            }

            String maxTokenStr = apiConfig.getProperty(ConfigPropertyKey.EMBEDDING_REMOTE_MAX_TOKEN);
            if (maxTokenStr != null) {
                try {
                    int maxToken = Integer.parseInt(maxTokenStr);
                    if (maxToken <= 0) {
                        throw new IllegalArgumentException("invalid config '" + ConfigPropertyKey.EMBEDDING_REMOTE_MAX_TOKEN + "=" + maxTokenStr + "': " + maxToken + " < 0");
                    }
                    requestMaxToken = maxToken;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("invalid config '" + ConfigPropertyKey.EMBEDDING_REMOTE_MAX_TOKEN + "=" + maxTokenStr + "': " + e.getMessage(), e);
                }
            } else {
                requestMaxToken = DEFAULT_REQUEST_MAX_TOKEN;
            }

            var tokenizerInitResult = createAndInitTokenizer(requestHeaders);

            tokenizer = tokenizerInitResult.getTokenizerFuc();

            //emit event to inject tokenizer
            tokenizerToInject = tokenizerInitResult.getTokenizer();


            initialized = true;
        }
    }


    @Override
    public void inited() {
        if (tokenizerToInject instanceof DCheckComponent) {
            try {
                ((DCheckComponent) tokenizerToInject).inited();
            } catch (Exception e) {
                throw new IllegalStateException("tokenizer inited fail: " + e.getMessage(), e);
            }
        }

        if (tokenizerToInject != null) {
            publishInjectTokenizerEvent(tokenizerToInject);
            tokenizerToInject = null;
        }
    }

    @SneakyThrows
    protected void publishInjectTokenizerEvent(Object tokenizer) {
        if (tokenizer == null) return;
        Class<?> eventClass = Class.forName("org.example.dcheck.api.FileProcessorTokenizerInjectionEvent");
        Method publisher = eventClass.getDeclaredMethod("publish", IEventEmitter.class, Object.class);
        publisher.invoke(null, DuplicateCheckingProvider.getInstance().getChecking(), tokenizer);
    }

    protected TokenizerInitResult createAndInitTokenizer(Headers requestHeaders) {
        Class<?> tokenizerClass;
        try {
            tokenizerClass = Class.forName("org.example.dcheck.impl.embedding.remote.BigModelTokenizer");
        } catch (Throwable e) {
            log.warn("tokenizer load fail. maybe miss some dependencies or you don not want to use that class: " + e.getMessage());
            return new TokenizerInitResult(text -> 0, null);
        }

        try {
            Object tokenizer = tokenizerClass.getConstructor(OkHttpClient.class, Headers.class).newInstance(client, requestHeaders);
            Method estimateTokenCountInText = tokenizerClass.getMethod("estimateTokenCountInText", String.class);
            Method tokenizerInitMethod = tokenizerClass.getMethod("init");
            estimateTokenCountInText.setAccessible(true);
            tokenizerInitMethod.setAccessible(true);

            try {
                tokenizerInitMethod.invoke(tokenizer);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new IllegalStateException("builtin tokenizer class init fail: " + e.getMessage(), e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("init tokenizer '" + tokenizerClass + "' fail: " + e.getTargetException().getMessage(), e.getTargetException());
            }

            return new TokenizerInitResult(text -> {
                try {
                    return ((int) estimateTokenCountInText.invoke(tokenizer, text));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw e.getTargetException() instanceof RuntimeException ? ((RuntimeException) e.getTargetException()) : new RuntimeException(e.getTargetException());
                }
            }, tokenizer);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new IllegalStateException("builtin tokenizer class init fail: " + e.getMessage(), e);
        }
    }

    @NotNull
    private List<Embedding> doRequest(List<String> input) throws Exception {
        try {
            return doPartition(input)
                    .stream()
                    .flatMap(part -> {
                        try (var response = client.newCall(embeddingRequestTemplate.newBuilder()
                                .method(
                                        "POST",
                                        RequestBody.create(
                                                (String) codec.serialize(
                                                        new CreateEmbeddingRequest(modelName, part, dimension),
                                                        String.class),
                                                Constant.JSON
                                        )
                                )
                                .build()).execute()) {
                            if (response.body() == null) {
                                throw new RuntimeException(new IOException("response body is null"));
                            }
                            var res = (CreateEmbeddingResponse) codec.deserialize(response.body().bytes(), CreateEmbeddingResponse.class);
                            if (res.getData().size() != input.size()) {
                                throw new RuntimeException(new IOException("response data size is not " + input.size()));
                            }
                            log.debug("document batch count {}. usage: {}", input.size(), res.getUsage());
                            return res.getData().stream().sorted(Comparator.comparingInt(IndexEmbeddingRecord::getIndex))
                                    .map(e -> Embedding.from(e.getEmbedding(), getName()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).collect(Collectors.toList());
        } catch (Throwable e) {
            if (e.getCause() instanceof IOException) throw (IOException) e.getCause();
            throw new RuntimeException(e);
        }

    }

    @Override
    public Embedding embedQuery(String query) throws Exception {
        return doRequest(Collections.singletonList(query)).get(0);
    }

    @NotNull
    private List<List<String>> doPartition(List<String> input) {
        var fixedTokenSizeList = new ArrayList<List<String>>();
        int totalToken = 0;
        fixedTokenSizeList.add(new ArrayList<>());
        for (var seg : input) {
            if ((totalToken += tokenizer.applyAsInt(seg)) > requestMaxToken) {
                fixedTokenSizeList.add(new ArrayList<>());
            }
            fixedTokenSizeList.get(fixedTokenSizeList.size() - 1).add(seg);
        }

        // repartition if needed
        return fixedTokenSizeList.stream().flatMap(part -> CollectionUtils.partition(part, maxInputLength).stream()).collect(Collectors.toList());
    }

    @Data
    protected static class TokenizerInitResult {
        private final ToIntFunction<String> tokenizerFuc;
        @Nullable
        private final Object tokenizer;
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws Exception {
        return doRequest(documents);
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws Exception {
        return doRequest(Arrays.asList(documents));
    }

    @Data
    protected static class CreateEmbeddingRequest {
        @NonNull
        private final String model;
        @NonNull
        private final List<String> input;
        private final Integer dimensions;
    }

    @Data
    protected static class CreateEmbeddingResponse {
        @NonNull
        private final String model;
        @NonNull
        private final List<IndexEmbeddingRecord> data;
        private final CallUsage usage;
    }

    @Data
    protected static class IndexEmbeddingRecord {
        private final int index;
        private final float @NonNull [] embedding;
    }

    @RequiredArgsConstructor
    protected static class CallUsage {
        private int completion_tokens;
        private int prompt_tokens;
        private int total_tokens;
    }

    @Override
    public String toString() {
        return "BigModelEmbeddingFunction(" +
                "modelName='" + modelName + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", dimension=" + dimension +
                ')';
    }
}
