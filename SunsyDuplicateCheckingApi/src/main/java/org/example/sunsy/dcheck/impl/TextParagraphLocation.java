package org.example.sunsy.dcheck.impl;

import lombok.Data;
import org.example.sunsy.dcheck.api.ParagraphLocation;

/**
 * Date 2025/02/25
 *
 * @author 三石而立Sunsy
 */
@Data
public class TextParagraphLocation implements ParagraphLocation {
    private final int rowIdx;
}
