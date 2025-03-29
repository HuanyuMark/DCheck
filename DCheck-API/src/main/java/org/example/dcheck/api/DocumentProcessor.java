package org.example.dcheck.api;

import org.example.dcheck.impl.TextParagraphMetadata;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * Date: 2025/2/25
 * process document. split document to small segment 'paragraph'
 * 负责进行文档切分等对文档进行处理的操作
 * Note: use the spring order mechanism to determine which processor supporting the same type should be used.
 *
 * @author 三石而立Sunsy
 * @see org.example.dcheck.spi.Providers#findAllImplementations(Class)
 */
public interface DocumentProcessor extends DCheckComponent {

    DocumentProcessor UNSUPPORTED = new DocumentProcessor() {
        @Override
        public boolean support(@NotNull DocumentType type) {
            return false;
        }

        @Override
        public Stream<SimpleParagraph> split(@NotNull Document document) {
            return Stream.empty();
        }
    };


    boolean support(@NotNull DocumentType type);

    /**
     * 文本切分
     */
    Stream<SimpleParagraph> split(@NotNull Document document);


    default Stream<UniversalParagraph> splitToParagraphs(@NotNull Document document) {
        return split(document).map(documentParagraph -> {
            if (documentParagraph.getParagraphType() == BuiltinParagraphType.TEXT) {
                return UniversalParagraph.builder()
                        .paragraph(documentParagraph)
                        .metadata(TextParagraphMetadata.builder()
                                .documentId(document.getId())
                                .location(documentParagraph.getLocation())
                                .build())
                        .build();
            }
            //TODO support add other type of paragraph
            throw new UnsupportedOperationException();
        });
    }
}
