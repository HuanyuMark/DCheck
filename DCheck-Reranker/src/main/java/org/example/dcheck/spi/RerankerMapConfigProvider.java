package org.example.dcheck.spi;

import lombok.Getter;

import java.util.Properties;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
public class RerankerMapConfigProvider implements DCheckProvider {
    @Getter(lazy = true)
    private static final RerankerMapConfigProvider instance = new RerankerMapConfigProvider();

    @Getter(lazy = true)
    private final Properties rerankingModelMap = Providers.loadConfig("reranking-model-map");

    protected RerankerMapConfigProvider() {
    }
}
