package org.example.dcheck.api;

import org.example.dcheck.spi.ConfigProvider;
import org.springframework.lang.Nullable;

import java.util.Properties;

/**
 * Date 2025/02/26
 * common interface to access all config in dcheck.
 * you can access the config instance by {@link ConfigProvider#getApiConfig()}
 * there are some loading mechanism to load config. see in the provider...
 * @see ConfigProvider
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public class ApiConfig extends Properties {
    public static final String EMBEDDING_MODEL_KEY = "relevancy-engine.model.embedding.name";

    public static final String DEFAULT_VALUE = "default";
    public static final String DB_VECTOR_TYPE = "relevancy-engine.type";
    public static final String DB_VECTOR_URL = "relevancy-engine.config.url";
    public static final String RERANKING_MODEL_KEY = "relevancy-engine.model.reranking.name";
    public static final String RERANKING_MODEL_URL = "relevancy-engine.model.reranking.url";


    public ApiConfig() {
    }

    public ApiConfig(Properties defaults) {
        super(defaults);
    }

    @Nullable
    public String getString(String key) {
        String o = getProperty(key);
        if (o == null) {
            throw new IllegalArgumentException("invalid ApiConfig '" + key + "=" + null + "'");
        }
        return o;
    }
}
