package org.example.dcheck.api;

import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * Date: 2025/2/25
 * process document. split document to small segment 'paragraph'
 * 负责进行文档切分等对文档进行处理的操作
 * Note: use spring order mechanism to determine which processor supporting same type should be used.
 * @author 三石而立Sunsy
 * @see org.example.dcheck.spi.Providers#findAllImplementations(Class)
 */
@SuppressWarnings("unused")
public interface DocumentProcessor {

    DocumentProcessor UNSUPPORTED = new DocumentProcessor() {
        @Override
        public boolean support(@NotNull DocumentType type) {
            return false;
        }

        @Override
        public Stream<DocumentParagraph> split(@NotNull Document document) {
            return Stream.empty();
        }
    };

    default void init() throws Exception {
    }


    boolean support(@NotNull DocumentType type);

    /**
     * 文本切分
     */
    Stream<DocumentParagraph> split(@NotNull Document document);
}
