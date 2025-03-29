package org.example.dcheck.spi;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.api.Document;
import org.example.dcheck.api.DocumentProcessor;
import org.example.dcheck.api.DocumentType;
import org.example.dcheck.api.SimpleParagraph;
import org.example.dcheck.exception.UnsupportedDocumentTypeException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@Slf4j
public class DocumentProcessorProvider implements DCheckProvider, DocumentProcessor {

    @Getter(lazy = true)
    private final static DocumentProcessorProvider instance = new DocumentProcessorProvider();

    @Getter(lazy = true)
    private final List<DocumentProcessor> implementations = Providers.findAllImplementations(DocumentProcessor.class);

    private final Map<DocumentType, DocumentProcessor> matchedCache = new ConcurrentHashMap<>();

    protected DocumentProcessorProvider() {
    }

    @Override
    public void init() throws Exception {
        for (DocumentProcessor processor : getImplementations()) {
            processor.init();
        }
    }

    @Override
    public void inited() throws Exception {
        for (DocumentProcessor processor : getImplementations()) {
            processor.inited();
        }
    }

    /**
     * Note: return {@link #UNSUPPORTED} if no processor supports the document type
     */
    public DocumentProcessor getProcessor(DocumentType type) {
        return matchedCache.computeIfAbsent(type, (documentType) -> {
            for (DocumentProcessor impl : getImplementations()) {
                if (impl.support(type)) {
                    log.info("[DocumentProcessor Spi Match]: assign processor '{}' to process document type '{}'", impl.getClass().getName(), documentType);
                    return impl;
                }
            }
            return UNSUPPORTED;
        });
    }

    @Override
    public boolean support(@NonNull DocumentType type) {
        return getProcessor(type) != UNSUPPORTED;
    }

    @Override
    public Stream<SimpleParagraph> split(@NonNull Document document) {
        DocumentProcessor processor = getProcessor(document.getDocumentType());
        if (processor == UNSUPPORTED) {
            throw new UnsupportedDocumentTypeException(document.getDocumentType());
        }
        return processor.split(document);
    }
}
