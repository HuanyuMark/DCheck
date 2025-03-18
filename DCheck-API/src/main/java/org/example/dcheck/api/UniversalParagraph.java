package org.example.dcheck.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Delegate;
import lombok.experimental.NonFinal;

/**
 * Date 2025/03/13
 *
 * @author 三石而立Sunsy
 */
@Value
@NonFinal
@Builder
@AllArgsConstructor
public class UniversalParagraph {

    @Delegate
    SimpleParagraph paragraph;

    ParagraphMetadata metadata;
}
