package org.example.dcheck.api;


import lombok.*;
import lombok.experimental.Delegate;

import java.util.List;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
@Data
@With
@Builder
@AllArgsConstructor
public class ParagraphRelevancyCreation {
    @NonNull
    private final String collectionId;
    @Singular("add")
    private final List<Record> batch;

    @Data
    @Builder
    @AllArgsConstructor
    public static class Record {
        @Delegate
        private final DocumentParagraph paragraph;
        private final ParagraphMetadata metadata;
    }
}
