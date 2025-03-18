package org.example.dcheck.api;

import lombok.*;
import lombok.experimental.Delegate;
import lombok.experimental.NonFinal;

import java.util.List;

/**
 * Date 2025/03/13
 *
 * @author 三石而立Sunsy
 */
@Value
@NonFinal
public class DuplicatePart {

    /**
     * the paragraph be compared(need to find duplicates)
     */
    @NonNull
    Paragraph paragraph;

    /**
     * all duplicate paragraphs comparison with the current paragraph {@link #paragraph}
     */
    @With
    @NonNull
    List<DuplicateParagraph> duplicates;

    @Value
    @Builder
    @NonFinal
    @AllArgsConstructor
    public static class DuplicateParagraph {

        /**
         * the duplicated paragraph
         */
        @NonNull
        @Delegate
        Paragraph paragraph;

        /**
         * relevancy score [0,1]
         * 相似度分数
         */
        @With
        @Getter
        double relevancy;
    }
}
