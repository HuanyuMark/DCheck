package org.example.dcheck.impl.embedding.remote;

import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeException;
import dev.failsafe.RetryPolicy;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.example.dcheck.api.ApiConfig;
import org.example.dcheck.api.Codec;
import org.example.dcheck.api.DCheckTokenizer;
import org.example.dcheck.spi.CodecProvider;
import org.example.dcheck.spi.ConfigProvider;
import org.springframework.util.ConcurrentLruCache;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Date 2025/03/11
 *
 * @author 三石而立Sunsy
 */
@Slf4j
@AllArgsConstructor
@SuppressWarnings("unused")
public class BigModelTokenizer implements DCheckTokenizer {
    protected static final String BASE_URL = "https://open.bigmodel.cn/api/paas/v4/tokenizer";

    protected static final String DEFAULT_MODEL_NAME = "glm-4-flash";

    @Getter
    @Setter
    @NonNull
    private OkHttpClient client;

    private Request requestTemplate;

    @With
    private String modelName;

    @Getter
    private Codec codec;

    private volatile boolean init;

    @Getter
    @Setter
    @NonNull
    private RetryPolicy<Object> requestPolicy = RetryPolicy.builder()
            .handle(IOException.class)
            .withMaxRetries(3)
            // 初始等待1s，最多30s,每次重试时间以2倍增长
            .withBackoff(Duration.ofSeconds(1), Duration.ofSeconds(5), 1.5)
            .build();

    private final ConcurrentLruCache<String, Integer> estimateCache = new ConcurrentLruCache<>(2000, this::doRequest);


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
        if (init) return;
        synchronized (this) {
            if (init) return;

            doInit();

            init = true;
        }
    }

    private void doInit() {
        ApiConfig apiConfig = ConfigProvider.getInstance().getApiConfig();
        String uncheckedBaseUrl = apiConfig.getProperty(ConfigPropertyKey.TOKENIZER_REMOTE_BASE_URL);
        HttpUrl baseUrl;
        if (uncheckedBaseUrl == null) {
            baseUrl = HttpUrl.get(BASE_URL);
        } else {
            try {
                baseUrl = HttpUrl.get(uncheckedBaseUrl);
            } catch (Exception e) {
                throw new IllegalArgumentException("invalid config '" + ConfigPropertyKey.TOKENIZER_REMOTE_BASE_URL + "=" + uncheckedBaseUrl + "': " + e.getMessage(), e);
            }
        }

        requestTemplate = requestTemplate.newBuilder().url(baseUrl).build();

        modelName = apiConfig.getProperty(ConfigPropertyKey.TOKENIZER_REMOTE_MODEL_NAME, DEFAULT_MODEL_NAME);
//        if (modelName == null) {
//            throw new IllegalArgumentException("missing required config '" + ConfigPropertyKey.TOKENIZER_REMOTE_MODEL_NAME + "'");
//        }

        if (codec == null) {
            codec = CodecProvider.getInstance()
                    .getCodecs()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("manual set codec before init(), otherwise list " + Codec.class + " provider in classpath"));
        }
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
                            log.warn("do tokenize request fail, response: " + response);
                            throw new IOException("do tokenize request fail, response: " + response);
                        }
                    });
        } catch (FailsafeException e) {
            log.error("do tokenize request fail: " + e.getCause().getMessage(), e.getCause());
            return 0;
        }
    }


    @Override
    public int estimateTokenCountInText(String text) {
        init();
        if (StringUtils.hasText(text)) {
            return estimateCache.get(text);
        }
        return 0;
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
