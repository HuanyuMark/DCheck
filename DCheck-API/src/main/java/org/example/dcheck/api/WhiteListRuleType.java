package org.example.dcheck.api;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Date: 2025/3/19
 * support type hint
 *
 * @author 三石而立Sunsy
 */
public interface WhiteListRuleType {

    /**
     * same as {@link ParagraphType} and {@link ParagraphLocationType}.
     * <p>
     * support codec work
     */
    Map<String, WhiteListRuleType> ALL_TYPES = new ConcurrentSkipListMap<>();

    @NotNull
    String name();

    @NotNull
    Class<?> getType();
}
