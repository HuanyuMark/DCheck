package org.example.dcheck.spi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Date: 2025/3/19
 *
 * @author 三石而立Sunsy
 */
public interface VirtualThreadProvider {
    ThreadFactory getThreadFactory();

    ExecutorService getExecutorService();
}
