package org.example.dcheck.util;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Date: 2025/3/11
 *
 * @author 三石而立Sunsy
 */
public class UtilConst {
    @SuppressWarnings("all")
    public static final CompletableFuture[] EMPTY_FUTURE_ARRAY = new CompletableFuture[0];
    public static final Type MAP_TYPE = new ParameterizedTypeReference<Map<String, Object>>() {
    }.getType();
    private static final CompletableFuture<?> emptyFuture = CompletableFuture.completedFuture(null);
    public final static ResourcePatternResolver RESOLVER = new PathMatchingResourcePatternResolver();

    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> emptyFuture() {
        return (CompletableFuture<T>) emptyFuture;
    }
}
