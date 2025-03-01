package org.example.dcheck.impl.codec.gson;

import com.google.gson.*;
import lombok.*;
import org.example.dcheck.api.BuiltinParagraphLocationType;
import org.example.dcheck.api.Codec;
import org.example.dcheck.api.ParagraphLocation;
import org.example.dcheck.api.ParagraphLocationType;

import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Date: 2025/3/1
 *
 * @author 三石而立Sunsy
 */
@Data
public class GsonCodec implements Codec {
    private final String name = "DCheck-Impl-Codec-Gson";

    @Getter
    @Setter
    @NonNull
    private Gson gson;

    @Getter
    private final GsonBuilder defaultGsonBuilder = new GsonBuilder();

    @Getter
    private final Map<String, ParagraphLocationType> paragraphLocationTypeMap = new HashMap<>(Arrays.stream(BuiltinParagraphLocationType.values()).map(e -> new AbstractMap.SimpleEntry<>(e.name(), e)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

    {
        setGson(defaultGsonBuilder
                .registerTypeAdapter(ParagraphLocation.class, (JsonDeserializer<ParagraphLocation>) (json, typeOfT, context) -> {
                    var obj = json.getAsJsonObject();
                    var type = (ParagraphLocationType) context.deserialize(obj.get("type"), ParagraphLocationType.class);
                    if (type.getIfSingleton() != null) {
                        return type.getIfSingleton();
                    }
                    return context.deserialize(json, type.type());
                })
                .registerTypeAdapter(ParagraphLocationType.class, (JsonSerializer<ParagraphLocationType>) (ParagraphLocationType src, Type typeOfSrc, JsonSerializationContext context) -> context.serialize(src.name()))
                .registerTypeAdapter(ParagraphLocationType.class, (JsonDeserializer<ParagraphLocationType>) (json, typeOfT, context) -> {
                    String type = json.getAsString();
                    var paragraphLocationType = paragraphLocationTypeMap.get(type);
                    if (paragraphLocationType == null) {
                        throw new IllegalStateException("unknown ParagraphLocationType: " + type);
                    }
                    return paragraphLocationType;
                })
                .create());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Target> Target convertTo(Object input, Object targetTypeHint) {
        if (!(targetTypeHint instanceof Type)) {
            throw new IllegalArgumentException("unsupported target type: " + targetTypeHint);
        }
        if (targetTypeHint instanceof Class) {
            if (JsonElement.class.isAssignableFrom((Class<?>) targetTypeHint))
                return (Target) gson.toJsonTree(input);
            if (String.class.isAssignableFrom((Class<?>) targetTypeHint))
                return (Target) gson.toJson(input);
        }
        return gson.fromJson(gson.toJsonTree(input), (Type) targetTypeHint);
    }
}
