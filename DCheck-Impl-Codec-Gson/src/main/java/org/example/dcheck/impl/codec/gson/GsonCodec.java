package org.example.dcheck.impl.codec.gson;

import com.google.gson.*;
import com.google.gson.internal.bind.JsonTreeWriter;
import com.google.gson.stream.JsonReader;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.api.*;
import org.springframework.core.ParameterizedTypeReference;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Date: 2025/3/1
 *
 * @author 三石而立Sunsy
 */
@Data
@Slf4j
public class GsonCodec implements Codec {
    private final String name = "DCheck-Impl-Codec-Gson";

    /**
     * access to underlying gson instance.
     */
    @Getter
    @Setter
    @NonNull
    private Gson gson;

    /**
     * you can reuse this builder to reuse the default configuration of gson.
     * change/add configuration of that builder, and then call setGson(builder.create())
     * to reset the gson instance.
     */
    @Getter
    private final GsonBuilder defaultGsonBuilder = new GsonBuilder();

    public GsonCodec() {
    }

    private static final Type MapType = new ParameterizedTypeReference<Map<String, Object>>() {
    }.getType();

    {
        setGson(defaultGsonBuilder
                .registerTypeAdapter(ParagraphLocation.class, (JsonDeserializer<ParagraphLocation>) (json, typeOfT, context) -> {
                    //unwrap
                    if (json.isJsonPrimitive()) {
                        return context.deserialize(getGson().toJsonTree(json.getAsString()), typeOfT);
                    }
                    var obj = json.getAsJsonObject();
                    var str = obj.get("type");
                    if (str == null) {
                        throw new IllegalArgumentException("unknown ParagraphLocationType: " + json);
                    }
                    var type = (ParagraphLocationType) context.deserialize(str, ParagraphLocationType.class);
                    if (type.getIfSingleton() != null) {
                        return type.getIfSingleton();
                    }
                    return context.deserialize(json, type.type());
                })
                .registerTypeAdapter(ParagraphMetadata.class, (JsonSerializer<ParagraphMetadata>) (ParagraphMetadata src, Type typeOfSrc, JsonSerializationContext context) -> context.serialize(src, MapType))
                .registerTypeAdapter(ParagraphMetadata.class, (JsonDeserializer<ParagraphMetadata>) (json, typeOfT, context) -> {
                    //unwrap
                    if (json.isJsonPrimitive()) {
                        return context.deserialize(getGson().toJsonTree(json.getAsString()), typeOfT);
                    }
                    var obj = json.getAsJsonObject();
                    JsonElement ptValue = obj.get("paragraphType");
                    if (ptValue == null) {
                        throw new IllegalArgumentException("unknown ParagraphMetadata: " + json);
                    }
                    var paragraphType = (ParagraphType) context.deserialize(ptValue, ParagraphType.class);
                    var paragraphLocationType = paragraphType.getMetadataClass();
                    @SuppressWarnings("unchecked")
                    var all = (Map<String, Object>) context.deserialize(json, MapType);
                    var ins = (ParagraphMetadata) context.deserialize(json, paragraphLocationType);
                    var extensions = paragraphType.createExtension(all, ins.getDocumentId(), ins.getLocation());
                    if (extensions != null) return extensions;
                    return ins;
                })
                .registerTypeAdapter(ParagraphType.class, (JsonSerializer<ParagraphType>) (ParagraphType src, Type typeOfSrc, JsonSerializationContext context) -> context.serialize(src.name()))
                .registerTypeAdapter(ParagraphType.class, (JsonDeserializer<ParagraphType>) (json, typeOfT, context) -> {
                    String str;
                    try {
                        str = json.getAsString();
                    } catch (UnsupportedOperationException e) {
                        throw new IllegalArgumentException("unknown ParagraphType: " + json);
                    }
                    var paragraphType = ParagraphType.ALL_TYPES.get(str);
                    if (paragraphType == null) {
                        log.warn("Check if forget to register that type instance to: {}.ALL_TYPES: throw Unknown ParagraphType: {}", ParagraphType.class, str);
                        throw new IllegalArgumentException("unknown ParagraphType: " + str);
                    }
                    return paragraphType;
                })
                .registerTypeAdapter(ParagraphLocationType.class, (JsonSerializer<ParagraphLocationType>) (ParagraphLocationType src, Type typeOfSrc, JsonSerializationContext context) -> context.serialize(src.name()))
                .registerTypeAdapter(ParagraphLocationType.class, (JsonDeserializer<ParagraphLocationType>) (json, typeOfT, context) -> {
                    String type;
                    try {
                        type = json.getAsString();
                    } catch (UnsupportedOperationException e) {
                        throw new IllegalArgumentException("unknown ParagraphLocationType: " + json);
                    }
                    var paragraphLocationType = ParagraphLocationType.ALL_TYPES.get(type);
                    if (paragraphLocationType == null) {
                        log.warn("Check if forget to register that type instance to: {}.ALL_TYPES: throw Unknown ParagraphLocationType: {}", ParagraphLocationType.class, type);
                        throw new IllegalArgumentException("unknown ParagraphLocationType: " + type);
                    }
                    return paragraphLocationType;
                })
                .create());
    }

    @Override
    public <Target> Target deserialize(Object input, Type targetType) {
        if (input instanceof String) {
            return gson.fromJson((String) input, targetType);
        }
        if (input instanceof Reader) {
            return gson.fromJson((Reader) input, targetType);
        }
        if (input instanceof JsonReader) {
            return gson.fromJson((JsonReader) input, targetType);
        }
        if (input instanceof JsonElement) {
            return gson.fromJson((JsonElement) input, targetType);
        }
        return convertTo(input, targetType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <SerializeTo> SerializeTo serialize(Object input, Type serializeTo) {
        if (serializeTo instanceof Class<?>) {
            if (String.class.isAssignableFrom((Class<?>) serializeTo)) {
                return (SerializeTo) gson.toJson(input);
            }
            if (Appendable.class.isAssignableFrom(((Class<?>) serializeTo))) {
                JsonTreeWriter writer = new JsonTreeWriter();
                gson.toJson(input, input.getClass(), writer);
                return (SerializeTo) writer;
            }
            if (JsonElement.class.isAssignableFrom((Class<?>) serializeTo)) {
                return (SerializeTo) gson.toJsonTree(input);
            }
        }
        return convertTo(input, serializeTo);
    }

    @Override
    public <Target> Target convertTo(Object input, Type targetType) {
        return gson.fromJson(gson.toJsonTree(input), targetType);
    }
}
