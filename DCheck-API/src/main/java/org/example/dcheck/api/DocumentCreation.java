package org.example.dcheck.api;

import lombok.Builder;
import lombok.Data;

import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
 */
@Data
@Builder
@SuppressWarnings("unused")
public class DocumentCreation {
    private final String documentId;
    private final DocumentType documentType;
    private final Supplier<InputStream> content;

    public InputStream getContent() {
        return content.get();
    }
}
