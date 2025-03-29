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
@Builder
@NonFinal
@AllArgsConstructor
public class ParagraphRelevancyQueryResult {

    @With
    @Singular
    List<DuplicatePart> duplicateParts;
}
