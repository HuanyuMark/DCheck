package org.example.sunsy.dcheck.impl;

import lombok.*;
import org.example.sunsy.dcheck.api.ParagraphRelevancyCreation;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Date 2025/02/25
 *
 * @author 三石而立Sunsy
 */
@Data
@With
@Builder
@AllArgsConstructor
public class TextParagraphRelevancyCreation implements ParagraphRelevancyCreation {
    private final String collectionId;
    @NonNull
    private final Supplier<InputStream> paragraph;
    @Singular("metadata")
    private final Map<String, Object> metadata;

    public InputStream getParagraph() {
        return paragraph.get();
    }
}
