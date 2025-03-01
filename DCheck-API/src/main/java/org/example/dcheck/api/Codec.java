package org.example.dcheck.api;

import java.lang.reflect.Type;

/**
 * Date: 2025/3/1
 * 负责数据类型转换：1. json/bson/protobuf转换到pojo的互转 2. pojo之间的互转
 * Note: 实现类需要支持api包内接口与其impl实现类的转换
 * @see org.example.dcheck.impl.codec.gson.GsonCodec
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface Codec {
    /**
     * info about this codec
     */
    String getName();


    /**
     * 1. do serialization/deserialization
     * 2. do type conversion
     */
    default <Target> Target convertTo(Object input, Type targetType) {
        return convertTo(input, (Object) targetType);
    }

    /**
     * 1. do serialization/deserialization
     * 2. do type conversion
     */
    <Target> Target convertTo(Object input, Object targetTypeHint);
}
