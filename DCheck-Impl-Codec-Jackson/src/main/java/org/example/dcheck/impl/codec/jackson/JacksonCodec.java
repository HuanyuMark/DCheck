package org.example.dcheck.impl.codec.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.example.dcheck.api.*;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Map;

/**
 * Date 2025/03/06
 *
 * @author 三石而立Sunsy
 */
@Slf4j
@Getter
public class JacksonCodec implements Codec {
    public static final Version VERSION = new Version(
            0,
            0,
            1,
            "DCheck-Impl-Codec-Jackson First Production Version",
            "org.example.dcheck",
            "DCheck-Impl-Codec-Jackson"
    );

    private final String name = "DCheck-Impl-Codec-Jackson";

    @Getter
    private ObjectMapper objectMapper;

    private final static com.fasterxml.jackson.databind.Module dcheckModule;

    private final static com.fasterxml.jackson.databind.Module parameterNamesModule = new ParameterNamesModule();

    private final static TypeReference<Map<String, Object>> MapType = new TypeReference<Map<String, Object>>() {
    };


    @SuppressWarnings("unused")
    public static class ParagraphMetadataMixin {

        // indicate 'ParagraphMetadata(String documentId, ParagraphLocation location)' is used for deserialization
        @JsonCreator
        public ParagraphMetadataMixin(String documentId, ParagraphLocation location) {
        }
    }

    @SuppressWarnings("unused")
    public interface NameIdentityMixin {
        @JsonValue
        String name();
    }


    static {
        dcheckModule = new SimpleModule(VERSION.getArtifactId(), VERSION)
                .addDeserializer(ParagraphLocation.class, new JsonDeserializer<ParagraphLocation>() {
                    @Override
                    public ParagraphLocation deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
                        if (jsonParser.getCurrentToken() == JsonToken.VALUE_STRING) {
                            var codec = (ObjectMapper) jsonParser.getCodec();
                            return codec.readValue(jsonParser.readValueAs(String.class), ParagraphLocation.class);
                        }
                        TreeNode tree = jsonParser.readValueAsTree();
                        TreeNode typeNode = tree.get("type");
                        if (!(typeNode instanceof TextNode)) {
                            throw new JsonParseException(jsonParser, "unknown ParagraphLocationType: " + typeNode);
                        }
                        var locationType = ParagraphLocationType.ALL_TYPES.get(((TextNode) typeNode).textValue());
                        if (locationType.getIfSingleton() != null) {
                            return locationType.getIfSingleton();
                        }
                        return jsonParser.getCodec().treeToValue(tree, locationType.type());
                    }
                })
                .setMixInAnnotation(ParagraphMetadata.class, ParagraphMetadataMixin.class)
                .addDeserializer(ParagraphMetadata.class, new JsonDeserializer<ParagraphMetadata>() {
                    @Override
                    public ParagraphMetadata deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                        if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
                            var codec = (ObjectMapper) p.getCodec();
                            return codec.readValue(p.readValueAs(String.class), ParagraphMetadata.class);
                        }
                        TreeNode treeNode = p.readValueAsTree();
                        TreeNode paragraphTypeNode = treeNode.get("paragraphType");
                        if (!(paragraphTypeNode instanceof TextNode)) {
                            throw new JsonParseException(p, "unknown ParagraphType: " + paragraphTypeNode);
                        }
                        var paragraphType = p.getCodec().treeToValue(paragraphTypeNode, ParagraphType.class);
                        var paragraphLocationType = paragraphType.getMetadataClass();
                        @SuppressWarnings("unchecked")
                        var all = p.getCodec().treeToValue(treeNode, (Class<Map<String, Object>>) MapType.getType());
                        var ins = p.getCodec().treeToValue(treeNode, paragraphLocationType);
                        var extensions = paragraphType.createExtension(all, ins.getDocumentId(), ins.getLocation());
                        if (extensions != null) return extensions;
                        return ins;
                    }
                })
                .addSerializer(ParagraphMetadata.class, new JsonSerializer<ParagraphMetadata>() {
                    @Override
                    public void serialize(ParagraphMetadata value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                        JsonSerializer<Object> serializer = serializers.findValueSerializer(serializers.getTypeFactory().constructType(MapType));
                        serializer.serialize(value, gen, serializers);
                    }
                })
                .setMixInAnnotation(ParagraphType.class, NameIdentityMixin.class)
                .addDeserializer(ParagraphType.class, new JsonDeserializer<ParagraphType>() {
                    @Override
                    public ParagraphType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                        if (p.currentToken() != JsonToken.VALUE_STRING) {
                            throw new JsonParseException(p, "unknown ParagraphType: " + p.currentValue());
                        }
                        String str = p.readValueAs(String.class);
                        var paragraphType = ParagraphType.ALL_TYPES.get(str);
                        if (paragraphType == null) {
                            log.warn("Check if forget to register that type instance to: {}.ALL_TYPES: throw Unknown ParagraphType: {}", ParagraphType.class, str);
                            throw new JsonParseException(p, "unknown ParagraphType: " + str);
                        }
                        return paragraphType;
                    }
                })
                .setMixInAnnotation(ParagraphLocationType.class, NameIdentityMixin.class)
                .addDeserializer(ParagraphLocationType.class, new JsonDeserializer<ParagraphLocationType>() {
                    @Override
                    public ParagraphLocationType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                        if (p.currentToken() != JsonToken.VALUE_STRING) {
                            throw new JsonParseException(p, "unknown ParagraphLocationType: " + p.currentValue());
                        }
                        String str = p.readValueAs(String.class);
                        var paragraphLocationType = ParagraphLocationType.ALL_TYPES.get(str);
                        if (paragraphLocationType == null) {
                            log.warn("Check if forget to register that type instance to: {}.ALL_TYPES: throw Unknown ParagraphLocationType: {}", ParagraphLocationType.class, str);
                            throw new JsonParseException(p, "unknown ParagraphLocationType: " + str);
                        }
                        return paragraphLocationType;
                    }
                });
    }

    {
        setObjectMapper(new ObjectMapper());
    }

    public void setObjectMapper(@NonNull ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        objectMapper.registerModule(parameterNamesModule);
        objectMapper.registerModule(dcheckModule);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public <Target> Target deserialize(Object input, Type targetType) throws IOException {
        if (input instanceof String) {
            return objectMapper.readValue((String) input, objectMapper.constructType(targetType));
        }
        if (input instanceof InputStream) {
            return objectMapper.readValue((InputStream) input, objectMapper.constructType(targetType));
        }
        if (input instanceof URL) {
            return objectMapper.readValue((URL) input, objectMapper.constructType(targetType));
        }
        if (input instanceof File) {
            return objectMapper.readValue((File) input, objectMapper.constructType(targetType));
        }
        if (input instanceof byte[]) {
            return objectMapper.readValue((byte[]) input, objectMapper.constructType(targetType));
        }
        if (input instanceof Reader) {
            return objectMapper.readValue((Reader) input, objectMapper.constructType(targetType));
        }
        if (input instanceof JsonNode) {
            return objectMapper.treeToValue((JsonNode) input, objectMapper.constructType(targetType));
        }
        if (input instanceof JsonParser) {
            return objectMapper.readValue((JsonParser) input, objectMapper.constructType(targetType));
        }
        if (input instanceof DataInput) {
            return objectMapper.readValue((DataInput) input, objectMapper.constructType(targetType));
        }
        return convertTo(input, targetType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <SerializeTo> SerializeTo serialize(Object input, Type serializeTo) throws IOException {
        if (serializeTo instanceof Class<?>) {
            if (CharSequence.class.isAssignableFrom((Class<?>) serializeTo)) {
                return (SerializeTo) objectMapper.writeValueAsString(input);
            }
            if (byte[].class.isAssignableFrom((Class<?>) serializeTo)) {
                objectMapper.writeValueAsBytes(input);
            }
            if (JsonNode.class.isAssignableFrom((Class<?>) serializeTo)) {
                return (SerializeTo) objectMapper.valueToTree(input);
            }
        }
        return convertTo(input, serializeTo);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Target> Target convertTo(Object input, Type targetType) throws IOException {
        if (input instanceof String) {
            return objectMapper.readValue((String) input, objectMapper.constructType(targetType));
        }
        if (targetType instanceof Class<?> && String.class.isAssignableFrom(((Class<?>) targetType))) {
            return (Target) objectMapper.writeValueAsString(input);
        }
        return objectMapper.convertValue(input, objectMapper.constructType(targetType));
    }

    @Override
    public <Target> Target convertTo(Object input, Object targetTypeHint) throws IOException {
        if (targetTypeHint instanceof TypeReference) {
            return objectMapper.treeToValue(objectMapper.valueToTree(input), objectMapper.constructType(((TypeReference<?>) targetTypeHint)));
        }
        if (targetTypeHint instanceof JavaType) {
            return objectMapper.treeToValue(objectMapper.valueToTree(input), (JavaType) targetTypeHint);
        }
        return Codec.super.convertTo(input, targetTypeHint);
    }
}
