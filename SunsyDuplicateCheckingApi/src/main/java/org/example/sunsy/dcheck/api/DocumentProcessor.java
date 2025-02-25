package org.example.sunsy.dcheck.api;

import java.util.stream.Stream;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface DocumentProcessor {

    boolean support(DocumentType type);

    /**
     * 文本切分
     */
    Stream<DocumentParagraph> split(Document document);
}
