package org.example.dcheck.api;

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
    private final List<RelevantParagraph> relevantParagraphs;
    @Singular("document")
    private final List<RelevantDocument> relevantDocuments;

    @Data
    @Builder
    public static class RelevantParagraph {
        @Singular("add")
        private final List<Paragraph> paragraph;
        private final double score;
    }

    @Data
    @Builder
    public static class RelevantDocument {
        private final String documentId;
        private final double score;
    }
}
