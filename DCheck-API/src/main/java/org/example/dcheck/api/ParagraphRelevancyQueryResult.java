package org.example.dcheck.api;

import lombok.*;
import lombok.experimental.Delegate;

import java.util.List;

/**
 * Date 2025/02/25
 *
 * @author 三石而立Sunsy
 */
@Data
@Builder
@SuppressWarnings("unused")
public class ParagraphRelevancyQueryResult {
    @With
    @Singular
    private final List<List<Record>> records;

    @Data
    @Builder
    public static class Record {
        @NonNull
        @Delegate
        private final Paragraph paragraph;
        @With
        @Getter
        private final double relevancy;
    }
}
