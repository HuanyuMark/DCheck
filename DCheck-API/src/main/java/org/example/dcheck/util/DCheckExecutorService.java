package org.example.dcheck.util;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.api.DCheckComponent;
import org.example.dcheck.spi.VirtualThreadProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

/**
 * Date: 2025/3/18
 * An executor service wrapped by {@link ExecutorService}.
 * it would run nicely in jdk21 and do the same work in legacy.
 * if virtual thread is available, the parallelism control of these class is difficult
 * (due to virtual thread use a jvm-shared scheduler to control application parallelism and user can
 * not control the scheduler directly)
 * <p>
 * so if you use virtual thread, you should control parallelism by yourself (by other mechanism such as set okhttp.Dispatcher in http call).
 *
 * @author 三石而立Sunsy
 */
@Slf4j
@SuppressWarnings("unused")
public class DCheckExecutorService implements ExecutorService, DCheckComponent {

    @Nullable
    protected static final ExecutorService defaultVirtualThreadExecutor;
    @Nullable
    protected static final ThreadFactory defaultVirtualThreadFactory;

    @Getter
    private static final DCheckExecutorService sharedExecutor = new DCheckExecutorService();

    static {
        ExecutorService candidateEs;
        ThreadFactory candidateThreadFactory;
        try {
            Class.forName("java.lang.VirtualThread");
            VirtualThreadProvider provider = loadProvider();
            candidateEs = provider.getExecutorService();
            candidateThreadFactory = provider.getThreadFactory();
        } catch (ClassNotFoundException e) {
            candidateEs = null;
            candidateThreadFactory = null;
        }

        defaultVirtualThreadExecutor = candidateEs;
        defaultVirtualThreadFactory = candidateThreadFactory;

        if (defaultVirtualThreadExecutor != null) {
            log.info("VirtualThread Is Available: set two system properties to customize: \n " +
                    "-Djdk.virtualThreadScheduler.parallelism=<default is core count> \n " +
                    "-Djdk.virtualThreadScheduler.maxPoolSize=<default is 256>");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                defaultShutdown(log, sharedExecutor);
            } catch (InterruptedException e) {
                throw new IllegalStateException("shutdown shared executor fail: " + e.getMessage(), e);
            }
        }));
    }

    @Getter
    @Setter
    @NonNull
    private static ThreadFactory sharedThreadFactory = defaultVirtualThreadFactory == null ? Thread::new : defaultVirtualThreadFactory;

    @Getter
    protected int concurrency = Runtime.getRuntime().availableProcessors() - 1;

    @Getter
    protected final boolean useVirtualThread;

    protected volatile ExecutorService executor;

    public DCheckExecutorService() {
        this(true);
    }

    public DCheckExecutorService(boolean useVirtualThreadIfAvailable) {
        useVirtualThread = useVirtualThreadIfAvailable && defaultVirtualThreadExecutor != null;
    }

    public static void defaultShutdown(Logger log, ExecutorService executor) throws InterruptedException {
        if (executor instanceof DCheckExecutorService) {
            if (((DCheckExecutorService) executor).executor == null) {
                return;
            }
        }

        if (executor == null) {
            log.warn("Executor Service Is Null. Ignore Shutdown Operation");
            return;
        }

        executor.shutdown();
        long start = System.currentTimeMillis();
        if (executor.awaitTermination(1, TimeUnit.MINUTES)) {
            log.info("Executor Service Closed Success. cost {}ms", System.currentTimeMillis() - start);
        } else {
            log.warn("Executor Service Closed Timeout. cost {}ms", System.currentTimeMillis() - start);
        }
    }

    private static VirtualThreadProvider loadProvider() {
        return StreamSupport.stream(ServiceLoader.load(VirtualThreadProvider.class, DCheckResourceClassLoader.getShared()).spliterator(), false)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no provider found"));
    }


    public ExecutorService getExecutor() {
        if (executor == null) {
            synchronized (this) {
                if (executor != null) return executor;
                init();
            }
        }
        return executor;
    }

    public void setExecutor(@NonNull ExecutorService executor) {
        this.executor = executor;
    }

    protected void initPlatform() {
        AtomicInteger idx = new AtomicInteger();
        ThreadFactory threadFactory = (r) -> {
            Thread thread = new Thread(r);
            thread.setName("dc-" + idx.getAndIncrement());
            return thread;
        };
        executor = new ThreadPoolExecutor(Math.max(concurrency / 2, 1), concurrency, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1024), threadFactory);
    }

    @Override
    public void init() {
        if (!useVirtualThread) {
            initPlatform();
            return;
        }

        executor = defaultVirtualThreadExecutor;
    }

    @Override
    public void close() {
        long start = System.currentTimeMillis();
        try {
            defaultShutdown(log, this);
        } catch (InterruptedException e) {
            throw new IllegalStateException("await executor termination fail: " + e.getMessage(), e);
        }
    }

    @Override
    public void execute(@NotNull Runnable command) {
        getExecutor().execute(command);
    }

    @Override
    public void shutdown() {
        getExecutor().shutdown();
    }

    @Override
    public @NotNull List<Runnable> shutdownNow() {
        return getExecutor().shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return getExecutor().isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return getExecutor().isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        return getExecutor().awaitTermination(timeout, unit);
    }

    @Override
    public String toString() {
        return getExecutor().toString();
    }

    @Override
    public @NotNull Future<?> submit(@NotNull Runnable task) {
        return getExecutor().submit(task);
    }

    @Override
    public @NotNull <T> Future<T> submit(@NotNull Runnable task, T result) {
        return getExecutor().submit(task, result);
    }

    @Override
    public @NotNull <T> Future<T> submit(@NotNull Callable<T> task) {
        return getExecutor().submit(task);
    }

    @Override
    public @NotNull <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return getExecutor().invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return getExecutor().invokeAny(tasks, timeout, unit);
    }

    @Override
    public @NotNull <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return getExecutor().invokeAll(tasks);
    }

    @Override
    public @NotNull <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        return getExecutor().invokeAll(tasks, timeout, unit);
    }
}
