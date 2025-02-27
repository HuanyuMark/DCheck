package org.example.dcheck.impl;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import org.example.dcheck.api.*;

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

    //TODO 需不需要在这里定义 从 metadata中获取location和documentId的逻辑？
//    public static TextParagraphBuilderExt extBuilder() {
//        return new TextParagraphBuilderExt();
//    }
//
//
//    public static class TextParagraphBuilderExt {
//        private final TextParagraphBuilder superBuilder = TextParagraph.builder();
//
//    }
}
