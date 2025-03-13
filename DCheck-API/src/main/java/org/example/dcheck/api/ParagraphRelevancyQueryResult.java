package org.example.dcheck.api;

import lombok.*;

import java.util.List;

/**
 * Date 2025/02/25
 *
 * @author 三石而立Sunsy
 */
@Data
@Builder
@SuppressWarnings("unused")
@AllArgsConstructor
public class ParagraphRelevancyQueryResult {
    @With
    @Singular
    private final List<DuplicatePart> duplicateParts;
}
