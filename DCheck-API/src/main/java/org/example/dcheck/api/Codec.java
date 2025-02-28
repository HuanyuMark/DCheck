package org.example.dcheck.api;

import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;

/**
 * Date: 2025/3/1
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface Codec<TransportData> {

    String getName();

    boolean supportTransportData(Object input);

    default Type getTansportDataType() {
        return ResolvableType.forType(this.getClass()).as(Codec.class).getGeneric(0).getType();
    }

    /**
     * deserialize
     */
    <T> T formTransportData(Type objType, Object input);

    /**
     * serialize
     */
    TransportData toTransportData(Object obj);
}
