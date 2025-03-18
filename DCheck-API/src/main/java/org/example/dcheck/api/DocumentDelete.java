package org.example.dcheck.api;

import lombok.*;
import lombok.experimental.NonFinal;

/**
 * Date 2025/02/25
 *
 * @author 三石而立Sunsy
 */
@With
@Value
@Builder
@NonFinal
@AllArgsConstructor
public class DocumentDelete {
    @NonNull
    String collectionId;

    @NonNull
    MetadataMatchCondition metadataMatchCondition;
}
