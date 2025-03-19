package org.example.dcheck.api;

import java.lang.annotation.*;

/**
 * Date 2025/03/12
 * mark this class should be loaded before access the class
 * 标记该类的类加载需要被提前
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface PreloadClass {

    /**
     * mark the method should be call after class loaded
     * <p>
     * Note: <strong>marked method should be static and no parameters</strong>
     *
     */
    @Inherited
    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface PreloadMethod {
    }
}
