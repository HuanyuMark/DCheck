package org.example.dcheck.util;

import lombok.Getter;
import lombok.Setter;

/**
 * Date 2025/03/14
 * control the runnable. make sure the runnable only run once
 *
 * @author 三石而立Sunsy
 */
@Getter
@SuppressWarnings("unused")
public class OnceRunner implements Runnable {

    private final Runnable initTask;

    private volatile boolean ran;

    @Setter
    private boolean successIsRan = true;

    @Setter
    private boolean failIsRan;

    public OnceRunner() {
        this(() -> {
        });
    }

    public OnceRunner(Runnable task) {
        this.initTask = task;
    }

    public static OnceRunner of(Runnable task) {
        return new OnceRunner(task);
    }

    public static OnceRunner of() {
        return new OnceRunner();
    }

    public void reset() {
        ran = false;
    }

    public void run(Runnable task) {
        runTemplate(task);
    }

    public void runExp(ExceptionRunnable task) throws Exception {
        runTemplate(task);
    }

    @Override
    public final void run() {
        runTemplate(this.initTask);
    }

    protected void runTemplate(Runnable task) {
        if (ran) return;
        synchronized (this) {
            if (ran) return;
            try {
                doRun(task);
                if (successIsRan) {
                    ran = true;
                }
            } catch (Throwable e) {
                if (failIsRan) {
                    ran = true;
                }
                throw e;
            }
        }
    }

    protected void runTemplate(ExceptionRunnable task) throws Exception {
        if (ran) return;
        synchronized (this) {
            if (ran) return;
            try {
                doRun(task);
                if (successIsRan) {
                    ran = true;
                }
            } catch (Throwable e) {
                if (failIsRan) {
                    ran = true;
                }
                throw e;
            }
        }
    }

    protected void doRun(Runnable task) {
        task.run();
    }

    protected void doRun(ExceptionRunnable task) throws Exception {
        task.run();
    }

    public interface ExceptionRunnable {
        void run() throws Exception;
    }
}
