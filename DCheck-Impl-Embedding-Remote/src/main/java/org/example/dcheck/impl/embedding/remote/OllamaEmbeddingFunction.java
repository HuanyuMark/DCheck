package org.example.dcheck.impl.embedding.remote;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.example.dcheck.api.Codec;
import org.example.dcheck.api.embedding.Embedding;
import org.example.dcheck.api.embedding.EmbeddingFunction;
import org.example.dcheck.spi.CodecProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class OllamaEmbeddingFunction implements EmbeddingFunction {
    public final static String DEFAULT_BASE_API = "http://localhost:11434/api/embed";
    public final static String DEFAULT_MODEL_NAME = "nomic-embed-text";
    private final Codec codec;
    private final String baseUrl;
    private final String modelName;
    @Getter
    private final Map<String, Object> details;
    private OkHttpClient client;
    private volatile boolean initialized = false;

    {
        codec = CodecProvider.getInstance().getCodecs().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No codec provider found"));
    }

    public OllamaEmbeddingFunction(String baseUrl, String modelName) {
        this.baseUrl = baseUrl == null ? DEFAULT_BASE_API : baseUrl;
        this.modelName = modelName == null ? DEFAULT_MODEL_NAME : modelName;
        Map<String, Object> details = new HashMap<>();
        details.put("baseUrl", baseUrl);
        details.put("modelName", modelName);
        this.details = Collections.unmodifiableMap(details);
    }

    private CreateEmbeddingResponse createEmbedding(CreateEmbeddingRequest req) throws Exception {
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
            try (InputStream in = response.body().byteStream()) {
                return codec.deserialize(in, CreateEmbeddingResponse.class);
            }
        }
    }

    @Override
    public void init() {
        if (initialized) return;
        synchronized (this) {
            if (initialized) return;

            log.info("apply base url '{}' model '{}'", baseUrl, modelName);
            client = OkHttpClientFactory.getInstance().create();
            // ping ollama server...

            initialized = true;
        }
    }


    @Override
    public Embedding embedQuery(String query) throws Exception {
        CreateEmbeddingResponse response = createEmbedding(
                new CreateEmbeddingRequest(modelName, Collections.singletonList(query))
        );
        return new Embedding(response.getEmbeddings().get(0));
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws Exception {
        CreateEmbeddingResponse response = createEmbedding(
                new CreateEmbeddingRequest(modelName, documents)
        );
        return response.getEmbeddings().stream().map(Embedding::from).collect(Collectors.toList());
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws Exception {
        return embedDocuments(Arrays.asList(documents));
    }

    @Data
    protected static class CreateEmbeddingRequest {
        @NonNull
        private final String model;
        @NonNull
        private final List<String> input;
    }

    @Data
    protected static class CreateEmbeddingResponse {
        @NonNull
        private final String model;
        @NonNull
        private final List<List<Float>> embeddings;
    }
}
