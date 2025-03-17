package org.example.dcheck.api;

import org.example.dcheck.spi.DCheckConfigProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;

import java.util.Properties;

/**
 * Date 2025/02/26
 * common interface to access all config in dcheck.
 * you can access the config instance by {@link DCheckConfigProvider#getDCheckConfig()}
 * there are some loading mechanism to load config. see in the provider...
 *
 * @author 三石而立Sunsy
 * @see DCheckConfigProvider
 */
@SuppressWarnings("unused")
public class DCheckConfig {
    public static final String EMBEDDING_MODEL_KEY = "relevancy-engine.model.embedding.name";

    public static final String DEFAULT_VALUE = "default";
    public static final String DB_VECTOR_TYPE = "relevancy-engine.type";
    public static final String DB_VECTOR_URL = "relevancy-engine.config.url";
    public static final String RERANKING_MODEL_KEY = "relevancy-engine.model.reranking.name";
    public static final String RERANKING_MODEL_URL = "relevancy-engine.model.reranking.url";

    private final ConversionService conversionService;

    private final Properties values = new Properties();

    public DCheckConfig(Properties defaults, ConversionService conversionService) {
        values.putAll(defaults);
        this.conversionService = conversionService;
    }

    @NotNull
    public String required(String key) {
        String o = values.getProperty(key);
        if (o == null) {
            throw new IllegalArgumentException("invalid config '" + key + "=" + null + "': missing required config");
        }
        return o;
    }

    @NotNull
    public String required(String key, String defaultValue) {
        String o = values.getProperty(key);
        if (o == null) {
            return defaultValue;
        }
        return o;
    }

    @NotNull
    public <T> T required(String key, Class<T> clazz) {
        String value = values.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("invalid config '" + key + "=" + null + "': missing required config");
        }
        return convert(clazz, key, value);
    }

    /**
     * > 0
     */
    public Integer requiredPositiveInt(String key) {
        Integer value = required(key, Integer.class);
        if (value > 0) return value;
        throw new IllegalArgumentException("invalid config '" + key + "=" + value + "': value should be > 0");
    }

    @NotNull
    public <T> T required(String key, @NotNull T defaultValue, Class<T> clazz) {
        String value = values.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return convert(clazz, key, value);
    }

    @NotNull
    public Boolean requiredEnable(String key) {
        return required(key, Boolean.class);
    }

    @Nullable
    public Boolean nullableEnable(String key) {
        return nullable(key, Boolean.class);
    }

    @Nullable
    public <T> T nullable(String key, Class<T> clazz) {
        String value = values.getProperty(key);
        if (value == null) {
            return null;
        }
        return convert(clazz, key, value);
    }

    /**
     * > 0 or null
     */
    public Integer nullablePositiveInt(String key) {
        Integer value = nullable(key, Integer.class);
        if (value == null) return null;
        if (value > 0) return value;
        throw new IllegalArgumentException("invalid config '" + key + "=" + value + "': value should be > 0");
    }

    @Nullable
    public String nullable(String key) {
        return values.getProperty(key);
    }

    protected <T> T convert(Class<T> clazz, String key, String value) {
        try {
            return conversionService.convert(value, clazz);
        } catch (ConversionException e) {
            throw new IllegalArgumentException("invalid config '" + key + "=" + value + "': fail to convert '" + value + "' to '" + clazz + "'", e);
        } catch (Throwable e) {
            throw new IllegalArgumentException("invalid config '" + key + "=" + value + "': required type is '" + clazz + "'", e);
        }
    }
}
