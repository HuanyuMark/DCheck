package org.example.dcheck.impl.embedding.remote;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.example.dcheck.api.ApiConfig;
import org.example.dcheck.api.Codec;
import org.example.dcheck.api.embedding.Embedding;
import org.example.dcheck.api.embedding.EmbeddingFunction;
import org.example.dcheck.spi.CodecProvider;
import org.example.dcheck.spi.ConfigProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Date: 2025/3/8
 *
 * @author 三石而立Sunsy
 */
@Slf4j
@Getter
@SuppressWarnings("unused")
public class BigModelEmbeddingFunction implements EmbeddingFunction {
    private static final String DEFAULT_MODEL_NAME = "embedding-3";
    private static final String DEFAULT_BASE_API = "https://open.bigmodel.cn/api/paas/v4/embeddings";


    private static final String DIMENSION_CONFIG = "relevancy-engine.model.embedding.remote.dimension";


    private final String modelName;
    private final String baseUrl;
    private final Codec codec;
    private Integer dimension;
    private OkHttpClient client;
    private Request embeddingRequestTemplate;

    {
        codec = CodecProvider.getInstance().getCodecs().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No codec provider found"));
    }


    public BigModelEmbeddingFunction(String modelName, String baseUrl) {
        this.modelName = modelName == null ? DEFAULT_MODEL_NAME : modelName;
        this.baseUrl = baseUrl == null ? DEFAULT_BASE_API : baseUrl;
    }

    public void setClient(@NonNull OkHttpClient client) {
        this.client = client;
    }

    @Override
    public void init() {
        if (getClient() == null) {
            setClient(OkHttpClientFactory.getInstance().create());
        }
        HttpUrl url = HttpUrl.parse(baseUrl);
        if (url == null) {
            throw new IllegalArgumentException("invalid base url '" + baseUrl + "'");
        }
        embeddingRequestTemplate = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", Constant.HTTP_USER_AGENT)
                .build();
        ApiConfig apiConfig = ConfigProvider.getInstance().getApiConfig();
        String dimensionValueStr = apiConfig.getProperty(DIMENSION_CONFIG);
        if (dimensionValueStr != null) {
            try {
                dimension = Integer.parseInt(dimensionValueStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid config '" + DIMENSION_CONFIG + "=" + dimensionValueStr + "': " + e.getMessage(), e);
            }
        }
    }

    @Override
    public Embedding embedQuery(String query) throws Exception {
        return doRequest(Collections.singletonList(query)).get(0);
    }

    @NotNull
    private List<Embedding> doRequest(List<String> input) throws IOException {
        try (var response = client.newCall(embeddingRequestTemplate.newBuilder()
                .method(
                        "POST",
                        RequestBody.create((String) codec.serialize(
                                        new CreateEmbeddingRequest(modelName, input, dimension),
                                        String.class),
                                Constant.JSON))
                .build()).execute()) {
            if (response.body() == null) {
                throw new IOException("response body is null");
            }
            var res = (CreateEmbeddingResponse) codec.deserialize(response.body().bytes(), CreateEmbeddingResponse.class);
            if (res.getData().size() != input.size()) {
                throw new IOException("response data size is not " + input.size());
            }
            log.debug("document batch count {}. usage: {}", input.size(), res.getUsage());
            return res.getData().stream().sorted(Comparator.comparingInt(IndexEmbeddingRecord::getIndex))
                    .map(e -> Embedding.from(e.getEmbedding(), getName())).collect(Collectors.toList());
        }
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
}
