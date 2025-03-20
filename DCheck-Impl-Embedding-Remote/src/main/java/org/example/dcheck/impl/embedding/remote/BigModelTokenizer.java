package org.example.dcheck.impl.embedding.remote;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeException;
import dev.failsafe.RetryPolicy;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.example.dcheck.api.Codec;
import org.example.dcheck.api.DCheckConfig;
import org.example.dcheck.api.DCheckTokenizer;
import org.example.dcheck.spi.CodecProvider;
import org.example.dcheck.spi.DCheckConfigProvider;
import org.example.dcheck.util.OnceRunner;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Date 2025/03/11
 *
 * @author 三石而立Sunsy
 */
@Slf4j
@AllArgsConstructor
@SuppressWarnings("unused")
public class BigModelTokenizer implements DCheckTokenizer {
    protected static final URL BASE_URL;

    static {
        try {
            BASE_URL = new URL("https://open.bigmodel.cn/api/paas/v4/tokenizer");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    protected static final String DEFAULT_MODEL_NAME = "glm-4-flash";

    private final OnceRunner runner = OnceRunner.of();
    @Getter
    @Setter
    @NonNull
    private OkHttpClient client;

    private Request requestTemplate;
    @With
    private String modelName;
    @Getter
    private Codec codec;
    @Getter
    @Setter
    @NonNull
    private RetryPolicy<Object> requestPolicy = RetryPolicy.builder()
            .handle(IOException.class)
            .withMaxRetries(3)
            // 初始等待1s，最多30s,每次重试时间以2倍增长
            .withBackoff(Duration.ofSeconds(1), Duration.ofSeconds(5), 1.5)
            .build();

    private LoadingCache<String, Integer> estimateCache;
    protected LoadingCache<String, Integer> shortSentenceCache;

    private final AtomicInteger cacheClearVersion = new AtomicInteger(Integer.MIN_VALUE);

    @Getter
    @Setter
    @NonNull
    private ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);
    @Getter
    private int estimateCacheSize = -1;

    @Getter
    private long estimateCacheExpireTimeMillis = -1;

    public BigModelTokenizer(@NonNull OkHttpClient client, @NonNull Headers requestHeaders) {
        this.client = client;
        // provide api-key and other required header in headers
        requestTemplate = new Request.Builder()
                .url(BASE_URL)
                .headers(requestHeaders)
                .build();
    }

    @Override
    public void init() {
        runner.run(this::doInit);
    }

    protected Integer doRequest(String text) {
        String body;
        DoTokenizeRequest req = new DoTokenizeRequest(modelName, Collections.singletonList(new ChatMessageData("user", text)));
        try {
            body = codec.serialize(req, String.class);
        } catch (IOException e) {
            throw new IllegalStateException("serialize DoTokenizeRequest '" + req + "' fail: " + e.getMessage(), e);
        }
        try {
            return Failsafe.with(requestPolicy)
                    .get(() -> {
                        try (Response response = client.newCall(requestTemplate.newBuilder()
                                .method("POST", RequestBody.create(body, Constant.JSON))
                                .build()).execute()) {
                            if (response.body() == null) {
                                throw new IOException("response body is null");
                            }
                            DoTokenizeResponse doTokenizeResponse;
                            if (response.isSuccessful()) {
                                try (InputStream in = response.body().byteStream()) {
                                    doTokenizeResponse = codec.deserialize(in, DoTokenizeResponse.class);
                                } catch (IOException e) {
                                    throw new IOException("deserialize DoTokenizeResponse fail, body: " + response.body().string() + ":" + e.getMessage(), e);
                                }
                                return doTokenizeResponse.getUsage().getPrompt_tokens();
                            }
                            log.warn("do tokenize request fail, response: {}", response);
                            throw new IOException("do tokenize request fail, response: " + response);
                        }
                    });
        } catch (FailsafeException e) {
            log.error("do tokenize request fail: {}", e.getCause().getMessage(), e.getCause());
            return 0;
        }
    }

    private void doInit() {
        DCheckConfig apiConfig = DCheckConfigProvider.getInstance().getDCheckConfig();
        HttpUrl baseUrl = apiConfig.required(ConfigPropertyKey.TOKENIZER_REMOTE_BASE_URL, Objects.requireNonNull(HttpUrl.get(BASE_URL)), HttpUrl.class);
        requestTemplate = requestTemplate.newBuilder().url(baseUrl).build();

        modelName = apiConfig.required(ConfigPropertyKey.TOKENIZER_REMOTE_MODEL_NAME, DEFAULT_MODEL_NAME);

        if (codec == null) {
            codec = CodecProvider.getInstance()
                    .getCodecs()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("manual set codec before init(), otherwise list " + Codec.class + " provider in classpath"));
        }

        estimateCache = Caffeine.newBuilder()
                .build(this::doRequest);
        shortSentenceCache = Caffeine.newBuilder()
                .build(this::doRequest);

        if (getEstimateCacheExpireTimeMillis() < 0) {
            setEstimateCacheExpireTimeMillis(apiConfig.requiredPositiveLong(ConfigPropertyKey.TOKENIZER_REMOTE_ESTIMATE_CACHE_EXPIRE_TIME));
        } else {
            setEstimateCacheExpireTimeMillis(getEstimateCacheExpireTimeMillis());
        }

        if (getEstimateCacheSize() < 0) {
            setEstimateCacheSize(apiConfig.requiredPositiveInt(ConfigPropertyKey.TOKENIZER_REMOTE_ESTIMATE_CACHE_SIZE));
        } else {
            setEstimateCacheSize(getEstimateCacheSize());
        }
    }

    @Override
    public int estimateTokenCountInText(String rawText) {
        init();
        if (!StringUtils.hasText(rawText)) {
            return 0;
        }

        String text = rawText.trim();

        if (text.length() < 5) {
            return Objects.requireNonNull(shortSentenceCache.get(text));
        }

        return Objects.requireNonNull(estimateCache.get(text));
    }

    public void setEstimateCacheExpireTimeMillis(long estimateCacheExpireTimeMillis) {
        if (estimateCacheExpireTimeMillis <= 0) {
            throw new IllegalArgumentException("estimateCacheExpireTimeMillis must be > 0");
        }
        this.estimateCacheExpireTimeMillis = estimateCacheExpireTimeMillis;
        if (estimateCache != null) {
            estimateCache.policy().expireAfterAccess().ifPresent(op -> op.setExpiresAfter(getEstimateCacheExpireTimeMillis(), TimeUnit.MILLISECONDS));
        }
        if (shortSentenceCache != null) {
            shortSentenceCache.policy().expireAfterAccess().ifPresent(op -> op.setExpiresAfter(getEstimateCacheExpireTimeMillis(), TimeUnit.MILLISECONDS));
        }
    }

    public void setEstimateCacheSize(int estimateCacheSize) {
        if (estimateCacheSize <= 0) {
            throw new IllegalArgumentException("estimateCacheSize must be > 0");
        }
        this.estimateCacheSize = estimateCacheSize;
        if (estimateCache != null) {
            estimateCache.policy().eviction().ifPresent(ev -> ev.setMaximum(getEstimateCacheSize()));
        }
        if (shortSentenceCache != null) {
            shortSentenceCache.policy().eviction().ifPresent(ev -> ev.setMaximum(getShortSentenceCacheSize(getEstimateCacheSize())));
        }
    }

    protected int getShortSentenceCacheSize(int estimateCacheSize) {
        return estimateCacheSize * 30;
    }

    @Override
    public int estimateTokenCountInMessage(ChatMessage message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int estimateTokenCountInMessages(Iterable<ChatMessage> messages) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int estimateTokenCountInToolSpecifications(Iterable<ToolSpecification> toolSpecifications) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int estimateTokenCountInToolExecutionRequests(Iterable<ToolExecutionRequest> toolExecutionRequests) {
        throw new UnsupportedOperationException();
    }

    public void setCodec(@NonNull Codec codec) {
        this.codec = codec;
    }

    @Data
    protected static class DoTokenizeRequest {
        @NonNull
        private final String model;
        @NonNull
        private final List<ChatMessageData> messages;
    }

    @Data
    protected static class ChatMessageData {
        private final String role;
        private final String content;
    }

    @Data
    protected static class DoTokenizeResponse {
        @NonNull
        private final Usage usage;
    }

    @Data
    protected static class Usage {
        private final int prompt_tokens;
    }
}
