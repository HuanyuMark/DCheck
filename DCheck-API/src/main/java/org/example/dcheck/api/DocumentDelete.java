package org.example.dcheck.api;

import lombok.*;

import java.util.Map;

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
    private final String collectionId;

    @NonNull
    private final MetadataMatchCondition metadataMatchCondition;

    @Data
    @Builder
    public static class MetadataMatchCondition {
        @Singular
        private final Map<String, String> eqs;
    }
}
