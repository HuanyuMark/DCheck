package org.example.sunsy.dcheck.spi;

import lombok.Getter;
import org.example.sunsy.dcheck.api.DocumentProcessor;
import org.example.sunsy.dcheck.api.DuplicateChecking;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
public class DocumentProcessorProvider implements DCheckProvider {
    @Getter(lazy = true)
    private final static DocumentProcessorProvider instance = new DocumentProcessorProvider();

    @Getter(lazy = true)
    private final DocumentProcessor dCheckImplementation = Providers.findImpl(DocumentProcessor.class,"dcheck.document.processor.impl");

    protected DocumentProcessorProvider() {
    }
}
