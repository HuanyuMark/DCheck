package org.example.dcheck.impl.embedding.remote;

import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.example.dcheck.api.embedding.Embedding;
import org.example.dcheck.api.embedding.EmbeddingFunction;
import org.example.dcheck.spi.ConfigProvider;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

/**
 * Date 2025/03/05
 *
 * @author 三石而立Sunsy
 */
@Slf4j
public class RemoteDelegateEmbeddingFunction implements EmbeddingFunction {

    private EmbeddingFunction target;

    public static String REMOTE_TYPE = "embedding.type";

    public static String REMOTE_BASE_URL = "embedding.remote.base-url";
    public static String REMOTE_MODEL_NAME = "embedding.remote.model-name";


    public void init() throws Exception {
        var apiConfig = ConfigProvider.getInstance().getApiConfig();
        String type = apiConfig.getProperty(REMOTE_TYPE);
        String baseUrl = apiConfig.getProperty(REMOTE_BASE_URL);
        String modelName = apiConfig.getProperty(REMOTE_MODEL_NAME);
        switch (type) {
            case "OLLAMA":
                if (StringUtils.hasText(baseUrl) && StringUtils.hasText(modelName)) {
                    target = new OllamaEmbeddingFunction(baseUrl, modelName);
                } else if (!StringUtils.hasText(modelName)) {
                    target = new OllamaEmbeddingFunction();
                } else {
                    throw new IllegalArgumentException("invalid config '" + REMOTE_TYPE + "=" + type + "': required baseUrl and modelName");
                }
                break;
            default:
                throw new IllegalArgumentException("invalid config '" + REMOTE_TYPE + "=" + type + "': unknown remote type");
        }
        target.init();
    }

    @Override
    public Embedding embedQuery(String query) throws Exception {
        return target.embedQuery(query);
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws Exception {
        return target.embedDocuments(documents);
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws Exception {
        return target.embedDocuments(documents);
    }

    @Override
    public List<Embedding> embedUnknownTypeDocuments(List<Supplier<InputStream>> documents) throws Exception {
        return target.embedUnknownTypeDocuments(documents);
    }
}
