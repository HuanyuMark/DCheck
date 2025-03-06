package org.example.dcheck.api;

import lombok.*;

/**
 * Date 2025/02/25
 *
 * @author 三石而立Sunsy
 */
@Data
@With
@Builder
@AllArgsConstructor
public class DocumentDelete {
    @NonNull
    private final String collectionId;

    @NonNull
    private final MetadataMatchCondition metadataMatchCondition;
}
