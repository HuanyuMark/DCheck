package org.example.dcheck.api;

import org.example.dcheck.impl.ContentMatchParagraphLocation;
import org.example.dcheck.impl.TextParagraphLocation;

/**
 * Date: 2025/2/28
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public enum BuiltinParagraphLocationType implements ParagraphLocationType {
    TEXT() {
        @Override
        public Class<? extends ParagraphLocation> type() {
            return TextParagraphLocation.class;
        }
    },
    CONTENT_MATCH() {
        @Override
        public Class<? extends ParagraphLocation> type() {
            return ContentMatchParagraphLocation.class;
        }
    };

    BuiltinParagraphLocationType() {
        ALL_TYPES.put(this.name(), this);
    }
}
