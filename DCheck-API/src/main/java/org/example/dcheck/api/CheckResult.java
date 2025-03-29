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
public class CheckResult {

    /**
     * 对于每一个段落，前 topKOfDocument 个最相似的文档
     */
    @Singular("paragraph")
    List<DuplicatePart> duplicateParts;

    /**
     * 前 topKOfDocument 个最相似的文档
     */
    @Singular("document")
    List<RelevantDocument> relevantDocuments;

    @Value
    @Builder
    @NonFinal
    @AllArgsConstructor
    public static class RelevantDocument {
        @NonNull
        String documentId;
        /**
         * total of all {@link #duplicateParts}.relevancy
         */
        double score;
    }
}
