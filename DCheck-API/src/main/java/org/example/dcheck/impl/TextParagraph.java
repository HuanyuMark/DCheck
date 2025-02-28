package org.example.dcheck.impl;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Delegate;
import org.example.dcheck.api.*;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Date 2025/02/25
 *
 * @author 三石而立Sunsy
 */
@Data
@Builder
public class TextParagraph implements Paragraph {
    private final DocumentCollection collection;
    @NonNull
    private final String documentId;
    @NonNull
    private final Supplier<TextContent> content;

    @NonNull
    private final ParagraphLocation location;

    @Getter(lazy = true)
    private final TextParagraphMetadata metadata = new TextParagraphMetadata(documentId, location);

    private final ParagraphType paragraphType = BuiltinParagraphType.TEXT;

    @Override
    public TextContent getContent() {
        return content.get();
    }

    public static MapBuilder mapBuilder() {
        return new MapBuilder();
    }

    public static class MapBuilder {
        @Delegate
        private final TextParagraphBuilder builder = builder();

        public MapBuilder fromFlat(Map<String, String> flatMap, BiFunction<String, Type, Object> deserializer) {
            String docId = flatMap.get("documentId");
            if (docId != null) {
                Object docIdObj = deserializer.apply(docId, String.class);
                if (!(docIdObj instanceof String)) {
                    throw new IllegalArgumentException("documentId is not String");
                }
                builder.documentId(((String) docIdObj));
            }
            String location = flatMap.get("location");
            if (location != null) {
                Object locationObj = deserializer.apply(location, ParagraphLocation.class);
                if (!(locationObj instanceof ParagraphLocation)) {
                    throw new IllegalArgumentException("location is not ParagraphLocation");
                }
                builder.location(((ParagraphLocation) locationObj));
            }
            return this;
        }
    }
}
