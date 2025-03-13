package org.example.dcheck.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Delegate;

/**
 * Date 2025/03/13
 *
 * @author 三石而立Sunsy
 */
@Data
@Builder
@AllArgsConstructor
public class UniversalParagraph {
    @Delegate
    private final SimpleParagraph paragraph;
    private final ParagraphMetadata metadata;
}
