package org.example.dcheck.impl;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;
import org.example.dcheck.api.BuiltinParagraphType;
import org.example.dcheck.api.DocumentCollection;
import org.example.dcheck.api.Paragraph;
import org.example.dcheck.api.ParagraphType;

import java.util.Map;
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
    @Singular("metadata")
    private final Map<String, Object> metadata;
    @NonNull
    private final TextParagraphLocation location;

    @Override
    public TextContent getContent() {
        return content.get();
    }

    @Override
    public ParagraphType getParagraphType() {
        return BuiltinParagraphType.TEXT;
    }
}
