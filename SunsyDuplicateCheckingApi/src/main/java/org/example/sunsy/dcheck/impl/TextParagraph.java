package org.example.sunsy.dcheck.impl;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;
import org.example.sunsy.dcheck.api.DocumentCollection;
import org.example.sunsy.dcheck.api.Paragraph;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Date 2025/02/25
 *
 * @author 三石而立Sunsy
 */
@Data
@Builder
public class TextParagraph implements Paragraph {
    private final DocumentCollection collection;
    @NonNull
    private final String documentId;
    @NonNull
    private final Supplier<InputStream> inputStream;
    @Singular("metadata")
    private final Map<String, Object> metadata;
    @NonNull
    private final TextParagraphLocation location;

    @Override
    public InputStream getInputStream() {
        return inputStream.get();
    }
}
