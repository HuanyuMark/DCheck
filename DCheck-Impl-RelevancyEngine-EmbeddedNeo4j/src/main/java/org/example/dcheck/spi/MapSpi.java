package org.example.dcheck.spi;

import lombok.Getter;
import org.example.dcheck.embedding.EmbeddingFunction;

/**
 * Date 2025/02/26
 * get service by a key. supported by MapConfigProvider, find a service class by the associated key and then return its instance
 *
 * @author 三石而立Sunsy
 */
public class MapSpi implements DCheckProvider {
    @Getter(lazy = true)
    private static final MapSpi instance = new MapSpi();

    public EmbeddingFunction getFunc(String modelKey) {
        return Providers.createService(EmbeddingFunctionMapConfigProvider.getInstance().getEmbeddingFuncMap(), "embedding model", modelKey);
    }
}
