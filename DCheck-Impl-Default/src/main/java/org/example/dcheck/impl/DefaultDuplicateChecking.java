package org.example.dcheck.impl;

import lombok.var;
import org.example.dcheck.api.*;
import org.example.dcheck.spi.ConfigProvider;
import org.example.dcheck.spi.MapSpi;
import org.springframework.cglib.beans.BeanMap;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
public class DefaultDuplicateChecking implements DuplicateChecking {
    @Override
    public void init() {
        var apiConfig = ConfigProvider.getInstance().getApiConfig();
        var embeddingModel = apiConfig.getProperty(ApiConfig.EMBEDDING_MODEL_KEY, ApiConfig.DEFAULT_VALUE);
        var embeddingFunction = MapSpi.getInstance().getFunc(embeddingModel);
        var relevancyEngine = MapSpi.getInstance().getRelevancyEngine(apiConfig.getProperty(ApiConfig.DB_VECTOR_TYPE, ApiConfig.DEFAULT_VALUE));
        var tasks = new ArrayList<CompletableFuture<?>>();
        tasks.add(CompletableFuture.runAsync(() -> {
            try {
                embeddingFunction.init();
            } catch (Exception e) {
                throw new IllegalStateException("init embedding function fail:", e);
            }
        }));

        tasks.add(CompletableFuture.runAsync(() -> {
            BeanMap beanMap = BeanMap.create(relevancyEngine);
            beanMap.put("embeddingFunction", embeddingFunction);
            try {
                relevancyEngine.init();
            } catch (Exception e) {
                throw new IllegalStateException("init relevancy engine fail:", e);
            }
        }));

        tasks.forEach(CompletableFuture::join);
    }

    @Override
    public ParagraphRelevancyEngine getRelevancyEngine() {

        return null;
    }

    @Override
    public CheckResult check(Check check, DocumentCollection collection) {
        return null;
    }
}
