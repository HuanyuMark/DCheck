package org.example.dcheck.api;

import lombok.*;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
@Data
@With
@Builder
@AllArgsConstructor
public class ParagraphRelevancyQuery {
    private final String collectionId;
    @NonNull
    private final List<Supplier<? extends Content>> paragraphs;
    @Builder.Default
    private final int topK = 5;
    @Singular("includeMetadata")
    private final Set<String> includeMetadata;

    public List<? extends Content> getParagraphs() {
        return paragraphs.stream().map(Supplier::get).collect(Collectors.toList());
    }
}
