package org.example.dcheck.impl.embedding.remote;

import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.example.dcheck.api.embedding.EmbeddingFunction;
import org.example.dcheck.spi.ConfigProvider;

import static org.example.dcheck.impl.embedding.remote.ConfigPropertyKey.*;

/**
 * Date 2025/03/05
 *
 * @author 三石而立Sunsy
 */
@Slf4j
public class RemoteDelegateEmbeddingFunction implements EmbeddingFunction {
    @Delegate
    private EmbeddingFunction target;

    public void init() throws Exception {
        val apiConfig = ConfigProvider.getInstance().getApiConfig();
        val type = apiConfig.getProperty(REMOTE_TYPE);
        val baseUrl = apiConfig.getProperty(REMOTE_BASE_URL);
        val modelName = apiConfig.getProperty(REMOTE_MODEL_NAME);

        target = determineFunc(type, baseUrl, modelName);
        target.init();
    }

    protected EmbeddingFunction determineFunc(String type, String baseUrl, String modelName) {
        switch (type == null ? "<null>" : type.toLowerCase()) {
            case "ollama":
                return new OllamaEmbeddingFunction(baseUrl, modelName);
            case "zhipu":
            case "zhi-pu":
            case "big-model":
            case "bigmodel":
                return new BigModelEmbeddingFunction(baseUrl, modelName);
            case "<null>":
            default:
                throw new IllegalArgumentException("invalid config '" + REMOTE_TYPE + "=" + type + "': unknown remote type");
        }
    }

    @Override
    public String toString() {
        return target.toString();
    }
}
