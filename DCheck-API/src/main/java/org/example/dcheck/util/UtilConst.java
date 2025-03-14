package org.example.dcheck.util;

import org.springframework.core.ParameterizedTypeReference;

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
    private static final CompletableFuture<?> emptyFuture = CompletableFuture.completedFuture(null);

    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> emptyFuture() {
        return (CompletableFuture<T>) emptyFuture;
    }

    public static final Type MAP_TYPE = new ParameterizedTypeReference<Map<String, Object>>() {
    }.getType();
}
