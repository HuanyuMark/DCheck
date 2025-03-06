package org.example.dcheck.impl.embedding.remote;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.example.dcheck.api.Codec;
import org.example.dcheck.api.embedding.Embedding;
import org.example.dcheck.api.embedding.EmbeddingFunction;
import org.example.dcheck.impl.embedding.remote.entity.OllamaCreateEmbeddingRequest;
import org.example.dcheck.impl.embedding.remote.entity.OllamaCreateEmbeddingResponse;
import org.example.dcheck.spi.CodecProvider;
import org.example.dcheck.spi.ConfigProvider;

import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class OllamaEmbeddingFunction implements EmbeddingFunction {
    public final static String DEFAULT_BASE_API = "http://localhost:11434/api/embed";
    public final static String DEFAULT_MODEL_NAME = "nomic-embed-text";
    private OkHttpClient client;
    private final Codec codec;

    public static final String CALL_TIME_OUT = "relevancy-engine.model.embedding.remote.timeout";


    {
        codec = CodecProvider.getInstance().getCodecs().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No codec provider found"));
    }

    private final String baseUrl;

    private final String modelName;

    @SuppressWarnings("unused")
    public OllamaEmbeddingFunction() {
        this(DEFAULT_BASE_API, DEFAULT_MODEL_NAME);
    }

    public OllamaEmbeddingFunction(String baseUrl, String modelName) {
        this.baseUrl = baseUrl == null ? DEFAULT_BASE_API : baseUrl;
        this.modelName = modelName == null ? DEFAULT_MODEL_NAME : modelName;

    }

    private OllamaCreateEmbeddingResponse createEmbedding(OllamaCreateEmbeddingRequest req) throws Exception {
        Request request = new Request.Builder()
                .url(baseUrl)
                .post(RequestBody.create((String) codec.serialize(req, String.class), Constant.JSON))
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", Constant.HTTP_USER_AGENT)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            if (response.body() == null) {
                throw new IOException("response body is null");
            }

            String responseData = response.body().string();

            return codec.deserialize(responseData, OllamaCreateEmbeddingResponse.class);
        }
    }

    @Override
    public void init() {
        log.info("apply base url '{}' model '{}'", baseUrl, modelName);
        var apiConfig = ConfigProvider.getInstance().getApiConfig();
        String timeout = apiConfig.getProperty(CALL_TIME_OUT);
        Duration timeoutDuration;
        try {
            timeoutDuration = Duration.parse(timeout);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("invalid config '" + CALL_TIME_OUT + "=" + timeout + "': " + e.getMessage(), e);
        }
        client = new OkHttpClient.Builder()
                .readTimeout(timeoutDuration)
                .build();
        // ping ollama server...
    }

    @Override
    public Embedding embedQuery(String query) throws Exception {
        OllamaCreateEmbeddingResponse response = createEmbedding(
                new OllamaCreateEmbeddingRequest(modelName, Collections.singletonList(query))
        );
        return new Embedding(response.getEmbeddings().get(0));
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws Exception {
        OllamaCreateEmbeddingResponse response = createEmbedding(
                new OllamaCreateEmbeddingRequest(modelName, documents)
        );
        return response.getEmbeddings().stream().map(Embedding::new).collect(Collectors.toList());
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws Exception {
        return embedDocuments(Arrays.asList(documents));
    }
}
