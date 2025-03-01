package org.example.dcheck.spi;

import lombok.Getter;
import org.example.dcheck.api.ParagraphRelevancyEngine;

/**
 * Date 2025/02/26
 * get service by a key. supported by MapConfigProvider, find a service class by the associated key and then return its instance
 *
 * @author 三石而立Sunsy
 */
public class RelevancyEngineMapProvider implements DCheckProvider {
    @Getter
    private static final RelevancyEngineMapProvider instance = new RelevancyEngineMapProvider();

    public ParagraphRelevancyEngine getRelevancyEngine(String relevancyEngineKey) {
        return Providers.createService(RelevancyEngineMapConfigProvider.getInstance().getRelevancyEngineMap(), "relevancy engine", relevancyEngineKey);
    }
}
