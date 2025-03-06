package org.example.dcheck.spi;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.api.Document;
import org.example.dcheck.api.DocumentParagraph;
import org.example.dcheck.api.DocumentProcessor;
import org.example.dcheck.api.DocumentType;

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
@SuppressWarnings("unused")
public class DocumentProcessorProvider implements DCheckProvider, DocumentProcessor {
    @Getter(lazy = true)
    private final static DocumentProcessorProvider instance = new DocumentProcessorProvider();

    @Getter(lazy = true)
    private final List<DocumentProcessor> implementations = Providers.findAllImplementations(DocumentProcessor.class);

    private final Map<DocumentType, DocumentProcessor> matchedCache = new ConcurrentHashMap<>();

    protected DocumentProcessorProvider() {
    }

    public DocumentProcessor getProcessor(DocumentType type) {
        return matchedCache.computeIfAbsent(type, (documentType) -> {
            for (DocumentProcessor impl : getImplementations()) {
                try {
                    impl.init();
                } catch (Exception e) {
                    throw new IllegalStateException("init document processor '" + impl.getClass() + "' fail: ", e);
                }
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
    public Stream<DocumentParagraph> split(@NonNull Document document) {
        DocumentProcessor processor = getProcessor(document.getDocumentType());
        if (processor == UNSUPPORTED) {
            log.error("[DocumentProcessor Proxy]: proxy process fail: no found processor for type '{}'", document.getDocumentType());
        }
        return processor.split(document);
    }
}
