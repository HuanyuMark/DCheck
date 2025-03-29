package org.example.dcheck.api;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
 */
@Value
@Builder
@NonFinal
public class DocumentCreation {

    @NonNull
    String documentId;

    @NonNull
    DocumentType documentType;

    @NonNull
    Supplier<InputStream> content;

    public InputStream getContent() {
        return content.get();
    }
}
