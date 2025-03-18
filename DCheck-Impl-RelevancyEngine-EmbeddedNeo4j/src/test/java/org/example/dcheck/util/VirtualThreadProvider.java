package org.example.dcheck.util;

import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

/**
 * Date 2025/03/18
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public class VirtualThreadProvider implements Supplier<ThreadFactory> {
    @Override
    public ThreadFactory get() {
        return Thread.ofVirtual().name("dc-v-", 0).factory();
    }
}
