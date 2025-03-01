package org.example.dcheck.api;

import java.lang.reflect.Type;

/**
 * Date: 2025/3/1
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface Codec {

    String getName();

    default <Target> Target convertTo(Object input, Type targetType) {
        return convertTo(input, (Object) targetType);
    }

    <Target> Target convertTo(Object input, Object targetTypeHint);
}
