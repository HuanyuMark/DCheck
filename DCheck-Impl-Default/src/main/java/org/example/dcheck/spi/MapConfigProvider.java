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
    private final Properties embeddingFuncMap = Providers.loadConfig("embedding-model-map");

    @Getter(lazy = true)
    private final Properties relevancyEngineMap = Providers.loadConfig("relevancy-engine-map");

    protected MapConfigProvider() {
    }
}
