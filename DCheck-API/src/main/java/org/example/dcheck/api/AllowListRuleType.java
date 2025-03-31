package org.example.dcheck.api;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Date: 2025/3/19
 * support type hint
 *
 * @author 三石而立Sunsy
 */
public interface AllowListRuleType extends Serializable, EntityProvider<AllowListRule> {

    /**
     * same as {@link ParagraphType} and {@link ParagraphLocationType}.
     * <p>
     * support codec work
     */
    Map<String, AllowListRuleType> ALL_TYPES = new ConcurrentSkipListMap<>();

    @NotNull
    String name();

}
