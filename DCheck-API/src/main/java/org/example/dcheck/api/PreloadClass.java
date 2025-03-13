package org.example.dcheck.api;

import java.lang.annotation.*;

/**
 * Date 2025/03/12
 * 标记该类的类加载需要被提前
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface PreloadClass {

    // 标记某个该类的某个静态方法会在预加载完成后（类加载完成后）被运行
    @Inherited
    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface PreloadMethod {
    }
}
