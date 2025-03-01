package org.example.dcheck.api;

import lombok.Getter;
import org.example.dcheck.impl.TextParagraph;
import org.example.dcheck.impl.TextParagraphMetadata;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
 */
@Getter
public enum BuiltinParagraphType implements ParagraphType {
    TEXT(TextParagraphMetadata.class, TextParagraph.class),
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
