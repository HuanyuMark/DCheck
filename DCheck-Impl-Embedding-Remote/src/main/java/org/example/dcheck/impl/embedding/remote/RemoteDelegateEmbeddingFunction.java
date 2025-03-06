package org.example.dcheck.impl.embedding.remote;

import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.example.dcheck.api.embedding.Embedding;
import org.example.dcheck.api.embedding.EmbeddingFunction;
import org.example.dcheck.spi.ConfigProvider;

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

    public static final String REMOTE_TYPE = "relevancy-engine.model.embedding.remote.type";

    public static final String REMOTE_BASE_URL = "relevancy-engine.model.embedding.remote.base-url";
    public static final String REMOTE_MODEL_NAME = "relevancy-engine.model.embedding.remote.model-name";


    public void init() throws Exception {
        var apiConfig = ConfigProvider.getInstance().getApiConfig();
        String type = apiConfig.getProperty(REMOTE_TYPE);
        String baseUrl = apiConfig.getProperty(REMOTE_BASE_URL);
        String modelName = apiConfig.getProperty(REMOTE_MODEL_NAME);
        switch (type) {
            case "OLLAMA":
                target = new OllamaEmbeddingFunction(baseUrl, modelName);
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
