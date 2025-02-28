package org.example.dcheck.impl;

import com.google.gson.JsonElement;
import lombok.Data;
import org.example.dcheck.api.Codec;

import java.io.Reader;
import java.lang.reflect.Type;

/**
 * Date: 2025/3/1
 *
 * @author 三石而立Sunsy
 */
@Data
public class GsonCodec implements Codec<String> {
    private final String name = "DCheck-Impl-Default Gson";

    @Override
    public boolean supportTransportData(Object input) {
        return input instanceof String ||
                input instanceof Reader || input instanceof JsonElement;
    }

    @Override
    public <T> T formTransportData(Type objType, Object input) {
        if (input instanceof String) {
            return SerializerSupport.getGson().fromJson(((String) input), objType);
        }
        if (input instanceof Reader) {
            return SerializerSupport.getGson().fromJson(((Reader) input), objType);
        }
        if (input instanceof JsonElement) {
            return SerializerSupport.getGson().fromJson(((JsonElement) input), objType);
        }
        throw new IllegalArgumentException("unsupported input type: " + input.getClass().getName());
    }

    @Override
    public String toTransportData(Object obj) {
        return SerializerSupport.getGson().toJson(obj);
    }
}
