package org.example.dcheck.api;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Date: 2025/3/11 17:44
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface IEventEmitter extends AutoCloseable {

    <E> void addListener(Class<E> event, Function<E, @NotNull CompletableFuture<?>> listener);

    <E> void addOnceListener(Class<E> event, Function<E, @NotNull CompletableFuture<?>> listener);

    <E> void addSyncListener(Class<E> event, Consumer<E> cb);

    <E> void removeListener(Class<E> event, Function<E, @NotNull CompletableFuture<?>> listener);

    <E> void removeSyncListener(Class<E> event, Consumer<E> cb);

    CompletableFuture<?> emitEvent(Object event);

    <T> CompletableFuture<?> emitEvent(Class<T> evnetClass, T event);
}
