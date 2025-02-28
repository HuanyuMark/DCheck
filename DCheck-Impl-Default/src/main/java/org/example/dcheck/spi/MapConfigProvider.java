package org.example.dcheck.spi;

import lombok.Getter;

import java.util.Properties;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
public class MapConfigProvider implements DCheckProvider {
    @Getter(lazy = true)
    private static final MapConfigProvider instance = new MapConfigProvider();

    @Getter(lazy = true)
    private final Properties relevancyEngineMap = Providers.loadConfig("relevancy-engine-map");

    @Getter(lazy = true)
    private final Properties rerankingModelMap = Providers.loadConfig("reranking-model-map");

    public Properties getEmbeddingFuncMap() {
        return EmbeddingFunctionMapConfigProvider.getInstance().getEmbeddingFuncMap();
    }

    protected MapConfigProvider() {
    }
}
