package org.example.dcheck.spi;

import lombok.Getter;
import org.example.dcheck.api.Reranker;

/**
 * Date 2025/02/26
 * get service by a key. supported by MapConfigProvider, find a service class by the associated key and then return its instance
 *
 * @author 三石而立Sunsy
 */
public class RerankerMapProvider implements DCheckProvider {
    @Getter(lazy = true)
    private static final RerankerMapProvider instance = new RerankerMapProvider();

    public Reranker getReranker(String rerankerKey) {
        return Providers.createService(RelevancyEngineMapConfigProvider.getInstance().getRelevancyEngineMap(), "reranker", rerankerKey);
    }
}
