package org.example.dcheck.api;

import org.springframework.core.ParameterizedTypeReference;

import java.lang.reflect.Type;

/**
 * Date: 2025/3/1
 * 负责数据类型转换：1. json/bson/protobuf转换到pojo的互转 2. pojo之间的互转
 * Note: 实现类需要支持api包内接口与其impl实现类的转换
 *
 * @author 三石而立Sunsy
 * @see org.example.dcheck.impl.codec.gson.GsonCodec
 */
@SuppressWarnings("unused")
public interface Codec {
    /**
     * info about this codec
     */
    String getName();


    /**
     * 1. do serialization/deserialization<br>
     * 2. do type conversion<br>
     */
    <Target> Target convertTo(Object input, Type targetType);


    default <Target> Target convertTo(Object input, ParameterizedTypeReference<?> targetType) {
        return convertTo(input, targetType.getType());
    }
    /**
     * 1. do serialization/deserialization<br>
     * 2. do type conversion<br>
     * all impl should support hint: {@link Type}/{@link org.springframework.core.ParameterizedTypeReference}
     */
    default <Target> Target convertTo(Object input, Object targetTypeHint) {
        if (targetTypeHint instanceof ParameterizedTypeReference) {
            return convertTo(input, (ParameterizedTypeReference<?>) targetTypeHint);
        }
        if (!(targetTypeHint instanceof Type)) {
            throw new IllegalArgumentException("unsupported target type: " + targetTypeHint);
        }
        return convertTo(input, (Type) targetTypeHint);
    }
}
