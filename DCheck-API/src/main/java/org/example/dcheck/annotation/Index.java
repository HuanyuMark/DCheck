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
    IndexType value() default IndexType.NORMAL;

    enum IndexType {
        /**
         * 唯一索引
         */
        UNIQUE,
        /**
         * 普通索引
         */
        NORMAL,
        PRIMARY
    }

    @lombok.Value
    class Value {
        IndexType value;

        public static Value of(Index index) {
            return new Value(index.value());
        }
    }
}
