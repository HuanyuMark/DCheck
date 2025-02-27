package org.example.dcheck.impl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.api.Document;
import org.example.dcheck.api.DocumentParagraph;
import org.example.dcheck.api.DocumentProcessor;
import org.example.dcheck.api.DocumentType;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * Date: 2025/2/27
 *
 * @author 三石而立Sunsy
 */
@Slf4j
@Getter
public abstract class SpiSafeDocumentProcessor implements DocumentProcessor {
    private DocumentProcessor unsafe;

    @Override
    public void init() throws Exception {
        if (unsafe != null) return;
        synchronized (this) {
            if (unsafe != null) return;
            Class<?> candidate;
            try {
                candidate = Class.forName(getUnsafeDocumentProcessorClassName());
            } catch (Throwable e) {
                log.warn("[Spi Safe Load]: fail to load class '{}'", getUnsafeDocumentProcessorClassName(), e);
                return;
            }
            if (!DocumentProcessor.class.isAssignableFrom(candidate)) {

            }
//            candidate.getConstructor().newInstance();
//            unsafe.init();
        }
    }

    abstract String getUnsafeDocumentProcessorClassName();

    @Override
    public boolean support(@NotNull DocumentType type) {
        return unsafe != null && unsafe.support(type);
    }

    @Override
    public Stream<DocumentParagraph> split(@NotNull Document document) {
        return unsafe == null ? Stream.empty() : unsafe.split(document);
    }
}
