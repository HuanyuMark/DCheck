package org.example.dcheck.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Date: 2025/2/28
 * supply type info to {@link Codec}. support {@link ParagraphLocation} conversion
 * @author 三石而立Sunsy
 */
public interface ParagraphLocationType {

    /**
     * 在实例化ParagraphLocationType的实例时，请将该实例的name作为key，该实例作为value，放入ALL_TYPES中，
     * 以支持{@link Codec}转换
     */
    Map<String, ParagraphLocationType> ALL_TYPES = new ConcurrentHashMap<>();

    String name();

    Class<? extends ParagraphLocation> type();

    default ParagraphLocation getIfSingleton() {
        return null;
    }
}
