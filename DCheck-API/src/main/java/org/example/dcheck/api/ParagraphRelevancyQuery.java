package org.example.dcheck.api;

import lombok.*;
import org.jetbrains.annotations.Nullable;

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
    private final String documentId;
    //TODO 优化这种document本身就在collection中的场景
    /**
     * 仅当documentId指代的document存在于collectionId指代的collection中时，才允许为null
     * */
    private final List<Supplier<? extends Content>> paragraphs;
    @Builder.Default
    private final int topK = 5;
    /**
     * return all metadata if empty
     */
    @Singular("includeMetadataField")
    private final Set<String> includeMetadata;

    @Nullable
    public List<? extends Content> getParagraphs() {
        if(paragraphs == null) return null;
        return paragraphs.stream().map(Supplier::get).collect(Collectors.toList());
    }
}
