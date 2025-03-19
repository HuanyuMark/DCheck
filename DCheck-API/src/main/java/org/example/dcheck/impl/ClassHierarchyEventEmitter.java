package org.example.dcheck.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.api.IEventEmitter;
import org.example.dcheck.util.UtilConst;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Date: 2025/3/11 17:45
 *
 * @author 三石而立Sunsy
 */
@Slf4j
@SuppressWarnings("unused")
public class ClassHierarchyEventEmitter implements IEventEmitter {
    protected final Map<Class<?>, Set<Function<?, CompletableFuture<?>>>> bus = initBus();
    private final Map<Class<?>, List<Class<?>>> eventClassCache = new ConcurrentHashMap<>();

    // 为了避免在eventClassCache.computeIfAbsent()的mapper中递归调用search所以需要单独将生成逻辑以及其递归部分抽离出来
    protected List<Class<?>> doSearchClass(Class<?> startSearch) {
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> search = startSearch; search != Object.class && search != null; search = search.getSuperclass()) {
            result.add(search);
            Class<?>[] interfaces = search.getInterfaces();
            result.addAll(Arrays.asList(interfaces));
            for (Class<?> inter : interfaces) {
                result.addAll(doSearchClass(inter));
            }
        }
        return result;
    }

    protected List<Class<?>> searchEventClass(Class<?> startSearch) {
        return eventClassCache.computeIfAbsent(startSearch, this::doSearchClass);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<?> emitEvent(@NotNull Object event) {
        return emitEvent((Class<? super Object>) event.getClass(), event);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<?> emitEvent(Class<T> evnetClass, T event) {
        List<Class<?>> eventClasses = searchEventClass(evnetClass);

        List<Set<Function<?, CompletableFuture<?>>>> allEventLis = new ArrayList<>(1);
        for (Class<?> evtClass : eventClasses) {
            Set<Function<?, CompletableFuture<?>>> lis = bus.get(evtClass);
            if (lis != null) {
                allEventLis.add(lis);
            }
        }
        if (allEventLis.isEmpty() || allEventLis.get(0).isEmpty()) {
            log.warn("emitter: no listener for event type: {}, content: {}", eventClasses, event);
            return UtilConst.emptyFuture();
        }
        List<CompletableFuture<?>> result = new ArrayList<>(allEventLis.size());
        for (Set<Function<?, CompletableFuture<?>>> lis : allEventLis) {
            result.add(CompletableFuture.allOf(lis.stream().flatMap(l -> {
                try {
                    return Stream.of(((Function<Object, CompletableFuture<?>>) l).apply(event));
                } catch (Exception e) {
                    log.error("event '{}'\nlistener error", event, e);
                }
                return Stream.of();
            }).toArray(CompletableFuture[]::new)));
        }
        return CompletableFuture.allOf(result.toArray(UtilConst.EMPTY_FUTURE_ARRAY));
    }

    @Override
    public <E> void addListener(Class<E> event, Function<E, CompletableFuture<?>> listener) {
        bus.computeIfAbsent(event, e -> initCallbackSet()).add(listener);
    }

    @Override
    public <E> void addOnceListener(Class<E> event, Function<E, @NotNull CompletableFuture<?>> listener) {
        Object[] wrapper = {null};
        @SuppressWarnings("unchecked")
        Function<E, CompletableFuture<?>> cb = e -> {
            listener.apply(e);
            removeListener((Class<E>) e.getClass(), (Function<E, CompletableFuture<?>>) wrapper[0]);
            return UtilConst.emptyFuture();
        };
        wrapper[0] = cb;
        addListener(event, cb);
    }


    protected Map<Class<?>, Set<Function<?, CompletableFuture<?>>>> initBus() {
        return new HashMap<>();
    }

    protected Set<Function<?, CompletableFuture<?>>> initCallbackSet() {
        return new HashSet<>();
    }

    @Override
    public <E> void addSyncListener(Class<E> event, Consumer<E> cb) {
        addListener(event, e -> {
            cb.accept(e);
            return UtilConst.emptyFuture();
        });
    }

    @Override
    public <E> void removeListener(Class<E> event, Function<E, @NotNull CompletableFuture<?>> listener) {
        Set<Function<?, CompletableFuture<?>>> ls = bus.get(event);
        if (ls == null) {
            log.warn("remove: no listener for event {}", event);
            return;
        }
        ls.remove(listener);
    }

    @Override
    public <E> void removeSyncListener(Class<E> event, Consumer<E> cb) {
        //如果cb与addSyncListener中的cb一致，则生成的lambda实例是同一个，则可以正确移除对应的Function
        removeListener(event, e -> {
            cb.accept(e);
            return UtilConst.emptyFuture();
        });
    }

    @Override
    public void close() {
        bus.values().forEach(Set::clear);
        bus.clear();
        evictCache();
    }

    public void evictCache(Class<?> event) {
        eventClassCache.remove(event);
    }

    public void evictCache() {
        eventClassCache.clear();
    }
}
