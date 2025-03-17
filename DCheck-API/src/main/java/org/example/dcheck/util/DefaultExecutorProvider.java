package org.example.dcheck.util;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Date: 2025/3/18
 * TODO 可以自定义默认线程池
 *
 * @author 三石而立Sunsy
 */
@Slf4j
@SuppressWarnings("unused")
public class DefaultExecutorProvider {
    // may be customized
    @Getter
    protected static DefaultExecutorProvider instance = new DefaultExecutorProvider();
    @Getter
    public final Executor localExecutor = Runnable::run;
    @Getter
    @Setter
    @NonNull
    protected ThreadFactory threadFactory;
    @Getter
    protected Executor executor = determineExecutor();
    @Getter
    protected int concurrency = Runtime.getRuntime().availableProcessors() - 1;
    protected BlockingQueue<Runnable> blockingQueue;

    protected DefaultExecutorProvider() {
        if (executor != localExecutor) {
            log.info("Use Virtual Thread");
        }
    }

    public BlockingQueue<Runnable> getBlockingQueue() {
        if (blockingQueue != null) return blockingQueue;
        synchronized (this) {
            if (blockingQueue != null) return blockingQueue;
            blockingQueue = new ArrayBlockingQueue<>(1024);
        }
        return blockingQueue;
    }

    public synchronized void setBlockingQueue(BlockingQueue<Runnable> queue) {
        if (executor != localExecutor) {
            if (!(executor instanceof ThreadPoolExecutor)) {
                executor = new ThreadPoolExecutor(0, concurrency, 0, TimeUnit.MILLISECONDS, queue);
            } else {
                ThreadPoolExecutor old = (ThreadPoolExecutor) executor;
                executor = new ThreadPoolExecutor(old.getCorePoolSize(), old.getMaximumPoolSize(), old.getKeepAliveTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS, queue);
            }
        }
        blockingQueue = queue;
    }

    @SneakyThrows
    private Executor determineExecutor() {
        try {
            Class.forName("java.lang.VirtualThread");
        } catch (ClassNotFoundException e) {
            AtomicInteger idx = new AtomicInteger();
            threadFactory = (r) -> {
                Thread thread = new Thread(r);
                thread.setName("dc-" + idx.getAndIncrement());
                return thread;
            };
            return localExecutor;
        }

        @SuppressWarnings("all")
        Method ofVirtual = Thread.class.getMethod("ofVirtual");
        Object builder = ofVirtual.invoke(null);
        Method builder$name = builder.getClass().getMethod("name", String.class);
        builder = builder$name.invoke(builder, "dc-v-");
        Method builder$factory = builder.getClass().getMethod("factory");
        threadFactory = (ThreadFactory) builder$factory.invoke(builder);

        @SuppressWarnings("all")
        Method method = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
        return ((Executor) method.invoke(null));
    }

    public synchronized void setConcurrency(int concurrency) {
        if (concurrency <= 0) {
            throw new IllegalArgumentException("concurrency must be > 0");
        }
        this.concurrency = concurrency;
        if (executor != localExecutor) {
            if (!(executor instanceof ThreadPoolExecutor)) {
                executor = new ThreadPoolExecutor(0, concurrency, 0, TimeUnit.MILLISECONDS, getBlockingQueue());
            } else {
                ((ThreadPoolExecutor) executor).setMaximumPoolSize(concurrency);
            }
        } else {

        }
    }
}
