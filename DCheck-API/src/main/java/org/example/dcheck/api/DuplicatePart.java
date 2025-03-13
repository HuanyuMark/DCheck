package org.example.dcheck.api;

import lombok.*;
import lombok.experimental.Delegate;

import java.util.List;

/**
 * Date 2025/03/13
 *
 * @author 三石而立Sunsy
 */
@Data
public class DuplicatePart {
    @NonNull
    private final Paragraph paragraph;
    @With
    @NonNull
    private final List<DuplicateParagraph> duplicates;

    @Data
    @Builder
    @AllArgsConstructor
    public static class DuplicateParagraph {
        @NonNull
        @Delegate
        private final Paragraph paragraph;
        @With
        @Getter
        private final double relevancy;
    }
}
