package org.example.dcheck.api;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;
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
    @Singular
    private final List<Record> records;

    @Data
    @Builder
    public static class Record {
        @NonNull
        @Delegate
        private final Paragraph paragraph;
        private final double relevancy;
    }
}
