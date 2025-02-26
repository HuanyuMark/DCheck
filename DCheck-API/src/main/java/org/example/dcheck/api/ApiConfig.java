package org.example.dcheck.api;

import org.jetbrains.annotations.Nullable;

import java.util.Properties;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public class ApiConfig extends Properties {
    public static final String EMBEDDING_MODEL_KEY = "embedding.model";

    public static final String DEFAULT_VALUE = "default";
    public static final String DB_VECTOR_TYPE = "db.vector.type";
    public static final String DB_VECTOR_URL = "db.vector.url";


    public ApiConfig() {
    }

    public ApiConfig(Properties defaults) {
        super(defaults);
    }

    @Nullable
    public String getString(String key) {
        String o = getProperty(key);
        if (o == null) {
            throw new IllegalArgumentException("invalid ApiConfig '" + key + "=" + o + "'");
        }
        return o;
    }
}
