package org.example.dcheck.api;

import lombok.Getter;
import org.example.dcheck.impl.CharSeqTextContent;
import org.example.dcheck.impl.ReaderTextContent;
import org.example.dcheck.impl.TextParagraph;
import org.example.dcheck.impl.TextParagraphMetadata;
import org.springframework.lang.Nullable;

import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
 */
@Getter
public enum BuiltinParagraphType implements ParagraphType, PreloadClass {
    TEXT(TextParagraphMetadata.class, TextParagraph.class) {
        @Override
        public @Nullable ParagraphMetadata createExtension(Map<String, Object> all, ParagraphMetadata parent) {
            if (all == null || all.isEmpty()) return null;
            if (all.size() == 2) return null;
            if (all.size() < 2) {
                throw new IllegalArgumentException("all must have contain documentId,location");
            }
            if (parent instanceof TextParagraphMetadata) return ((TextParagraphMetadata) parent).withOthers(all);
            return new TextParagraphMetadata(parent.getDocumentId(), parent.getLocation()).withOthers(all);
        }

        @Override
        public Paragraph createParagraph(Object content, ParagraphMetadata metadata) {
            if (metadata.getParagraphType() != TEXT) {
                throw new UnsupportedOperationException("unsupported paragraph type: " + metadata.getParagraphType());
            }
            return new TextParagraph(() -> convertContent(content), metadata);
        }

        private TextContent convertContent(Object content) {
            if (content instanceof TextContent) {
                return (TextContent) content;
            }
            if (content instanceof CharSequence) {
                return new CharSeqTextContent((CharSequence) content);
            }
            if (content instanceof Reader) {
                return new ReaderTextContent((Reader) content);
            }
            if (content instanceof InputStream) {
                return () -> ((InputStream) content);
            }
            throw new UnsupportedOperationException("unsupported content type: " + content.getClass());
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

    @PreloadMethod
    private static void preload() {
    }
}
