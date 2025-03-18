package org.example.dcheck.api;


import lombok.*;
import lombok.experimental.NonFinal;

import java.util.List;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
@With
@Value
@Builder
@NonFinal
@AllArgsConstructor
public class ParagraphRelevancyCreation {

    @NonNull
    String collectionId;

    @Singular("add")
    List<UniversalParagraph> batch;
}
