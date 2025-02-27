package org.example.dcheck.impl.fileprocessor;

import org.example.dcheck.api.*;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * Date: 2025/2/28
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public class TikaDocumentProcessor implements DocumentProcessor {
    @Override
    public boolean support(@NotNull DocumentType type) {
        return type == BuiltinDocumentType.PDF || type == BuiltinDocumentType.DOCX;
    }

    @Override
    public Stream<DocumentParagraph> split(@NotNull Document document) {

        return null;
    }
}
