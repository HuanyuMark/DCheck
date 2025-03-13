package org.example.dcheck.api;

import lombok.*;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Set;

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
    @NonNull
    private final String collectionId;
    @NonNull
    private final String documentId;
    //TODO 优化这种document本身就在collection中的场景
    /**
     * 仅当documentId指代的document存在于collectionId指代的collection中时，才允许为null
     */
    @Nullable
    private final List<UniversalParagraph> paragraphs;
    @Builder.Default
    private final int topK = 5;

    /**
     * 如果相关性分数小于minRelevancy，则该段落会被忽略，不会参与后续的相似度计算
     */
    //TODO skip <= minRelevancy
    private final double minRelevancy;

    /**
     * return all metadata if empty
     */
    @Singular("includeMetadataField")
    private final Set<String> includeMetadata;
}
