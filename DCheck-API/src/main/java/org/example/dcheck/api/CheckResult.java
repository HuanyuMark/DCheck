package org.example.dcheck.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

/**
 * Date 2025/02/25
 *
 * @author 三石而立Sunsy
 */
@Data
@Builder
@SuppressWarnings("unused")
public class CheckResult {
    @Singular("paragraph")
    // 对于每一个段落，前 topKOfDocument 个最相似的文档
    private final List<List<ParagraphRelevancyQueryResult.Record>> relevantParagraphs;
    // 前 topKOfDocument 个最相似的文档
    @Singular("document")
    private final List<RelevantDocument> relevantDocuments;

    @Data
    @Builder
    @AllArgsConstructor
    public static class RelevantDocument {
        private final String documentId;
        private final double score;
    }
}
