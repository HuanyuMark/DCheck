package org.example.dcheck.impl;

import lombok.Data;

/**
 * Date: 2025/3/11
 *
 * @author 三石而立Sunsy
 */
@Data
public abstract class CallerSensitiveEvent {
    protected final Class<?> emitSource;

    public CallerSensitiveEvent() {
        Thread currentThread = Thread.currentThread();
        StackTraceElement[] stackTrace = currentThread.getStackTrace();
        try {
            this.emitSource = currentThread.getContextClassLoader().loadClass(stackTrace[1].getClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("unexpected class not found", e);
        }
    }
}
