package org.example.dcheck.impl.embedding.remote;

import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.api.DCheckConfig;
import org.example.dcheck.api.embedding.Embedding;
import org.example.dcheck.api.embedding.EmbeddingFunction;
import org.example.dcheck.spi.DCheckConfigProvider;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.example.dcheck.impl.embedding.remote.ConfigPropertyKey.*;

/**
 * Date 2025/03/05
 *
 * @author 三石而立Sunsy
 */
@Slf4j
public class RemoteDelegateEmbeddingFunction implements EmbeddingFunction {
    private EmbeddingFunction target;

    private volatile boolean init;

    @Override
    public void init() throws Exception {
        if (init) return;
        synchronized (this) {
            if (init) return;

            DCheckConfig DCheckConfig = DCheckConfigProvider.getInstance().getDCheckConfig();
            String type = DCheckConfig.required(REMOTE_TYPE);
            String baseUrl = DCheckConfig.nullable(EMBEDDING_REMOTE_BASE_URL);
            String modelName = DCheckConfig.nullable(EMBEDDING_REMOTE_MODEL_NAME);

            target = determineFunc(type, baseUrl, modelName);
            target.init();

            init = true;
        }
    }

    @Override
    public void inited() throws Exception {
        target.inited();
    }

    @Override
    public String getName() {
        try {
            init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return target.getName();
    }

    @Override
    public Map<String, Object> getDetails() {
        try {
            init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return target.getDetails();
    }

    @Override
    public Embedding embedQuery(String query) throws Exception {
        try {
            init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return target.embedQuery(query);
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws Exception {
        try {
            init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return target.embedDocuments(documents);
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws Exception {
        try {
            init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return target.embedDocuments(documents);
    }

    @Override
    public List<Embedding> embedUnknownTypeDocuments(List<Supplier<InputStream>> documents) throws Exception {
        try {
            init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return target.embedUnknownTypeDocuments(documents);
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
