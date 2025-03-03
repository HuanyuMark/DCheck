package org.example.dcheck.spi;

import lombok.Getter;

import java.util.Properties;

/**
 * Date: 2025/3/1
 *
 * @author 三石而立Sunsy
 */
public class EmbeddingFunctionMapConfigProvider implements DCheckProvider {

    @Getter(lazy = true)
    private static final EmbeddingFunctionMapConfigProvider instance = new EmbeddingFunctionMapConfigProvider();


    @Getter(lazy = true)
    private final Properties embeddingFuncMap = Providers.loadConfig("embedding-model-map");
}
