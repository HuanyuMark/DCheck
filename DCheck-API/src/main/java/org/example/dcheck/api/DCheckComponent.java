package org.example.dcheck.api;

/**
 * Date 2025/03/11
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface DCheckComponent extends AutoCloseable {
    /**
     * 组件初始化。准备依赖、注册事件监听器
     */
    @SuppressWarnings("all")
    default void init() throws Exception {
    }

    /**
     * 所有组件的 {@link #init()}方法执行完毕后执行
     */
    @SuppressWarnings("all")
    default void inited() throws Exception {
    }

    @SuppressWarnings("all")
    default void close() throws Exception {
    }
}
