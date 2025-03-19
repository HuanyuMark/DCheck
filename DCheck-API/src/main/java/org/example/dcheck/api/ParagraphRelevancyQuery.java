package org.example.dcheck.api;

import lombok.*;
import lombok.experimental.NonFinal;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
@With
@Value
@Builder
@NonFinal
@AllArgsConstructor
public class ParagraphRelevancyQuery {

    @NonNull
    String collectionId;

    @NonNull
    String documentId;

    /**
     * 仅当documentId指代的document存在于collectionId指代的collection中时，才允许为null
     */
    @Nullable
    List<UniversalParagraph> paragraphs;
    @Builder.Default
    int topK = 5;

    /**
     * 如果相关性分数小于minRelevancy，则该段落会被忽略，不会参与后续的相似度计算
     */
    double minRelevancy;

    /**
     * return all metadata if empty
     */
    @Singular("includeMetadataField")
    Set<String> includeMetadata;
}
