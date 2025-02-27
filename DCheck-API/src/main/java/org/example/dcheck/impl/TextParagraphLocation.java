package org.example.dcheck.impl;

import lombok.Data;
import org.example.dcheck.api.BuiltinParagraphLocationType;
import org.example.dcheck.api.ParagraphLocation;
import org.example.dcheck.api.ParagraphLocationType;

/**
 * Date 2025/02/25
 *
 * @author 三石而立Sunsy
 */
@Data
public class TextParagraphLocation implements ParagraphLocation {
    private final int rowIdx;

    @Override
    public ParagraphLocationType getType() {
        return BuiltinParagraphLocationType.TEXT;
    }
}
