package org.example.dcheck.annotation;

import java.lang.annotation.*;

/**
 * Date 2025/03/31
 * skip the using of the annotated element
 *
 * @author 三石而立Sunsy
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Ignore {
}
