package org.example.sunsy.dcheck.impl;

import lombok.*;
import org.example.sunsy.dcheck.api.ParagraphRelevancyQuery;

import java.io.InputStream;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Date 2025/02/25
 * 基于文本的相似度查询
 *
 * @author 三石而立Sunsy
 */
@Data
@With
@Builder
@AllArgsConstructor
public class TextParagraphRelevancyQuery implements ParagraphRelevancyQuery {
    private final String collectionId;
    @NonNull
    private final Supplier<InputStream> paragraph;
    @Builder.Default
    private final int topK = 5;
    @Singular("includeMetadata")
    private final Set<String> includeMetadata;

    public InputStream getParagraph() {
        return paragraph.get();
    }
}
