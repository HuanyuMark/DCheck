package org.example.dcheck.api;

import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
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
