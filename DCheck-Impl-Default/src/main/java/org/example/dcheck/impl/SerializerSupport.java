package org.example.dcheck.impl;

import com.google.gson.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.var;
import org.example.dcheck.api.BuiltinParagraphLocationType;
import org.example.dcheck.api.ParagraphLocation;
import org.example.dcheck.api.ParagraphLocationType;

import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Date 2025/02/27
 *
 * @author 三石而立Sunsy
 */
public class SerializerSupport {
    @Getter
    @Setter
    @NonNull
    private static Gson gson;

    @Getter
    private static final GsonBuilder defaultGsonBuilder = new GsonBuilder();

    @Getter
    private static final Map<String, ParagraphLocationType> paragraphLocationTypeMap = new HashMap<>(Arrays.stream(BuiltinParagraphLocationType.values()).map(e -> new AbstractMap.SimpleEntry<>(e.name(), e)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

    static {
        gson = defaultGsonBuilder
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
                .create();
    }
}
