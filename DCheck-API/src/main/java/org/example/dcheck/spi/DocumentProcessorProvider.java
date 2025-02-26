package org.example.dcheck.spi;

import lombok.Getter;
import org.example.dcheck.api.DocumentProcessor;
import org.example.dcheck.api.DocumentType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public class DocumentProcessorProvider implements DCheckProvider {
    @Getter(lazy = true)
    private final static DocumentProcessorProvider instance = new DocumentProcessorProvider();

    @Getter(lazy = true)
    private final List<DocumentProcessor> implementations = Providers.findAllImplementations(DocumentProcessor.class);

    private final Map<DocumentType, DocumentProcessor> matchedCache = new ConcurrentHashMap<>();

    protected DocumentProcessorProvider() {
    }

    public DocumentProcessor getProcessor(DocumentType type) {
        return matchedCache.computeIfAbsent(type, (key) -> {
            for (DocumentProcessor impl : getImplementations()) {
                if (impl.support(type)) {
                    return impl;
                }
            }
            return null;
        });
    }
}
