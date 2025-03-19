package org.example.dcheck.util;

import org.example.dcheck.spi.VirtualThreadProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Date 2025/03/18
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public class JdkVirtualThreadProvider implements VirtualThreadProvider {
    @Override
    public ThreadFactory getThreadFactory() {
        return Thread.ofVirtual().name("dc-v-", 0).factory();
    }

    @Override
    public ExecutorService getExecutorService() {
        return Executors.newThreadPerTaskExecutor(getThreadFactory());
    }
}
