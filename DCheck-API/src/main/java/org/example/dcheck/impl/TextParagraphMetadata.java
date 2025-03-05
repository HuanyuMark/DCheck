package org.example.dcheck.impl;

import lombok.ToString;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import org.example.dcheck.api.BuiltinParagraphType;
import org.example.dcheck.api.ParagraphLocation;
import org.example.dcheck.api.ParagraphType;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Date 2025/02/27
 *
 * @author 三石而立Sunsy
 */
@ToString
@SuperBuilder
@SuppressWarnings("unused")
public class TextParagraphMetadata extends AbstractParagraphMetadata {

    public TextParagraphMetadata(String documentId, ParagraphLocation location) {
        super(documentId, location);
    }

    public TextParagraphMetadata(Map<? extends String, ?> m, String documentId, ParagraphLocation location) {
        super(m, documentId, location);
    }

    @Override
    public ParagraphType getParagraphType() {
        return BuiltinParagraphType.TEXT;
    }

    public static MapBuilder mapBuilder() {
        return new MapBuilder();
    }


    public static class MapBuilder {
        @Delegate
        private final TextParagraphMetadataBuilder<?, ?> builder = TextParagraphMetadata.builder();

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

        public TextParagraphMetadata build() {
            return builder.build();
        }
    }
}
