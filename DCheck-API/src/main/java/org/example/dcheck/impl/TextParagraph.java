package org.example.dcheck.impl;

import lombok.*;
import org.example.dcheck.api.DocumentCollection;
import org.example.dcheck.api.Paragraph;
import org.example.dcheck.api.ParagraphMetadata;
import org.example.dcheck.api.TextContent;

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
    private final Supplier<TextContent> content;

    @With
    @Getter
    @NonNull
    private ParagraphMetadata metadata;

    @Override
    public TextContent getContent() {
        return content.get();
    }
}
