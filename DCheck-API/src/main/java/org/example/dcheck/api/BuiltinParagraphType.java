package org.example.dcheck.api;

import lombok.Getter;
import org.example.dcheck.impl.TextParagraph;
import org.example.dcheck.impl.TextParagraphMetadata;
import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
 */
@Getter
public enum BuiltinParagraphType implements ParagraphType {
    TEXT(TextParagraphMetadata.class, TextParagraph.class) {
        @Override
        public @Nullable ParagraphMetadata createExtension(Map<String, Object> all, String documentId, ParagraphLocation location) {
            if (all == null || all.isEmpty()) return null;
            if (all.size() == 2) return null;
            if (all.size() < 2) {
                throw new IllegalArgumentException("all must have contain documentId,location");
            }
            return new TextParagraphMetadata(all, documentId, location);
        }
    },
//    IMAGE,
    ;

    private final Class<? extends ParagraphMetadata> metadataClass;

    private final Class<? extends Paragraph> paragraphClass;

    BuiltinParagraphType(Class<? extends ParagraphMetadata> metadataClass, Class<? extends Paragraph> paragraphClass) {
        this.metadataClass = metadataClass;
        this.paragraphClass = paragraphClass;
        ALL_TYPES.put(name(), this);
    }
}
