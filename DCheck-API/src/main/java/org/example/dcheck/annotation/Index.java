package org.example.dcheck.annotation;

import java.lang.annotation.*;

/**
 * Date 2025/03/31
 *
 * @author 三石而立Sunsy
 */
@Inherited
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Index {
}
