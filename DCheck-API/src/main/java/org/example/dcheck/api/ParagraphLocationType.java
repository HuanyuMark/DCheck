package org.example.dcheck.api;

/**
 * Date: 2025/2/28
 *
 * @author 三石而立Sunsy
 */
public interface ParagraphLocationType {
    String name();

    Class<? extends ParagraphLocation> type();

    default ParagraphLocation getIfSingleton() {
        return null;
    }
}
