package org.example.dcheck.util;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.api.DCheckComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Date: 2025/3/18
 * An executor service wrapped by {@link ThreadPoolExecutor}.
 * it would run nicely in jdk21 and do the same work in legacy.
 * TODO use PerTask ExecutorService. pool es would lead to loose thread (dead blocked)
 *  so use virtual per task executor service and impl a purpose associated concurrency control
 *  is better
 * @author 三石而立Sunsy
 */
@Slf4j
@SuppressWarnings("unused")
public class DCheckExecutorService implements ExecutorService, DCheckComponent {

    @Nullable
    protected static final ThreadFactory defaultVirtualThreadFactory = initVirtualThreadFactory();

    static {
        if (defaultVirtualThreadFactory != null) {
            log.info("VirtualThread Is Available: set two system properties to customize: \n -Djdk.virtualThreadScheduler.parallelism=<default is core count> \n-Djdk.virtualThreadScheduler.maxPoolSize=<default is 256>");
        }
    }

    protected volatile BlockingQueue<Runnable> blockingQueue;

    @Getter
    protected int concurrency = Runtime.getRuntime().availableProcessors() - 1;
    @Getter
    protected boolean useVirtualThread;
    protected volatile ThreadPoolExecutor executor;


    public DCheckExecutorService() {
        this(true);
    }

    public DCheckExecutorService(boolean useVirtualThreadIfAvailable) {
        useVirtualThread = useVirtualThreadIfAvailable;
    }

    public static void defaultShutdown(Logger log, ExecutorService executor) throws InterruptedException {
        executor.shutdown();
        long start = System.currentTimeMillis();
        if (executor.awaitTermination(1, TimeUnit.MINUTES)) {
            log.info("Executor Service Closed Success. cost {}ms", System.currentTimeMillis() - start);
        } else {
            log.warn("Executor Service Closed Timeout. cost {}ms", System.currentTimeMillis() - start);
        }
    }

    private static Class<?> loadProviderClass() throws ClassNotFoundException {
        return DCheckResourceClassLoader.getShared().loadClass("org.example.dcheck.util.VirtualThreadProvider");
    }

    public BlockingQueue<Runnable> getBlockingQueue() {
        if (blockingQueue != null) return blockingQueue;
        synchronized (this) {
            if (blockingQueue != null) return blockingQueue;
            blockingQueue = new ArrayBlockingQueue<>(1024);
        }
        return blockingQueue;
    }

    public ThreadPoolExecutor getExecutor() {
        if (executor == null) {
            synchronized (this) {
                if (executor != null) return executor;
                init();
            }
        }
        return executor;
    }

    @Nullable
    @SneakyThrows
    @SuppressWarnings("unchecked")
    private static ThreadFactory initVirtualThreadFactory() {
        try {
            Class.forName("java.lang.VirtualThread");
            Class<?> providerClass = loadProviderClass();
            return Objects.requireNonNull(((Supplier<ThreadFactory>) providerClass.getConstructor().newInstance()).get());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public synchronized void setBlockingQueue(BlockingQueue<Runnable> queue) {
        blockingQueue = queue;
        ThreadPoolExecutor old = executor;
        executor = new ThreadPoolExecutor(old.getCorePoolSize(), old.getMaximumPoolSize(), old.getKeepAliveTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS, queue);
        old.shutdown();
    }

    protected void initPlatform() {
        AtomicInteger idx = new AtomicInteger();
        ThreadFactory threadFactory = (r) -> {
            Thread thread = new Thread(r);
            thread.setName("dc-" + idx.getAndIncrement());
            return thread;
        };
        executor = new ThreadPoolExecutor(Math.max(concurrency / 2, 1), concurrency, 10, TimeUnit.SECONDS, getBlockingQueue(), threadFactory);
    }

    @Override
    public void init() {
        if (!useVirtualThread || defaultVirtualThreadFactory == null) {
            initPlatform();
            useVirtualThread = false;
            return;
        }

        executor = new ThreadPoolExecutor(concurrency, Integer.MAX_VALUE, 100, TimeUnit.MILLISECONDS, getBlockingQueue(), Objects.requireNonNull(defaultVirtualThreadFactory));
        executor.allowCoreThreadTimeOut(true);
    }

    public synchronized void setConcurrency(int concurrency) {
        if (concurrency <= 0) {
            throw new IllegalArgumentException("concurrency must be > 0");
        }
        this.concurrency = concurrency;
        executor.setCorePoolSize(concurrency);
    }

    @Override
    public void close() {
        log.info("Closing Executor Service. shutdown manually is grateful");
        long start = System.currentTimeMillis();
        executor.shutdown();
        try {
            if (executor.awaitTermination(1, TimeUnit.MINUTES)) {
                log.info("Executor Service Closed Success. cost {}ms", System.currentTimeMillis() - start);
            } else {
                log.warn("Executor Service Closed Timeout. cost {}ms", System.currentTimeMillis() - start);
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("await executor termination fail: " + e.getMessage(), e);
        }
    }

    public void allowCoreThreadTimeOut(boolean value) {
        getExecutor().allowCoreThreadTimeOut(value);
    }

    public BlockingQueue<Runnable> getQueue() {
        return getExecutor().getQueue();
    }

    public long getTaskCount() {
        return getExecutor().getTaskCount();
    }

    public int getLargestPoolSize() {
        return getExecutor().getLargestPoolSize();
    }

    public long getKeepAliveTime(TimeUnit unit) {
        return getExecutor().getKeepAliveTime(unit);
    }

    public boolean remove(Runnable task) {
        return getExecutor().remove(task);
    }

    public long getCompletedTaskCount() {
        return getExecutor().getCompletedTaskCount();
    }

    public int getCorePoolSize() {
        return getExecutor().getCorePoolSize();
    }

    public void setCorePoolSize(int corePoolSize) {
        getExecutor().setCorePoolSize(corePoolSize);
    }

    public boolean isTerminating() {
        return getExecutor().isTerminating();
    }

    public void setKeepAliveTime(long time, TimeUnit unit) {
        getExecutor().setKeepAliveTime(time, unit);
    }

    public boolean prestartCoreThread() {
        return getExecutor().prestartCoreThread();
    }

    public void purge() {
        getExecutor().purge();
    }

    public ThreadFactory getThreadFactory() {
        return getExecutor().getThreadFactory();
    }

    public void setThreadFactory(@NotNull ThreadFactory threadFactory) {
        getExecutor().setThreadFactory(threadFactory);
    }

    public int getMaximumPoolSize() {
        return getExecutor().getMaximumPoolSize();
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        getExecutor().setMaximumPoolSize(maximumPoolSize);
    }

    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return getExecutor().getRejectedExecutionHandler();
    }

    public void setRejectedExecutionHandler(@NotNull RejectedExecutionHandler handler) {
        getExecutor().setRejectedExecutionHandler(handler);
    }

    public boolean allowsCoreThreadTimeOut() {
        return getExecutor().allowsCoreThreadTimeOut();
    }

    public int getActiveCount() {
        return getExecutor().getActiveCount();
    }

    public int getPoolSize() {
        return getExecutor().getPoolSize();
    }

    public int prestartAllCoreThreads() {
        return getExecutor().prestartAllCoreThreads();
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
