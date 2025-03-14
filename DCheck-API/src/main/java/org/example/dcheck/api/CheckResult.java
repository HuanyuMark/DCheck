package org.example.dcheck.api;

import lombok.*;
import lombok.experimental.NonFinal;

import java.util.List;

/**
 * Date 2025/02/25
 *
 * @author 三石而立Sunsy
 */
@Value
@NonFinal
@Builder
@SuppressWarnings("unused")
public class CheckResult {
    @Singular("paragraph")
    // 对于每一个段落，前 topKOfDocument 个最相似的文档
    List<DuplicatePart> duplicateParts;
    // 前 topKOfDocument 个最相似的文档
    @Singular("document")
    List<RelevantDocument> relevantDocuments;

    @Value
    @Builder
    @NonFinal
    @AllArgsConstructor
    public static class RelevantDocument {
        @NonNull
        String documentId;
        double score;
    }
}
