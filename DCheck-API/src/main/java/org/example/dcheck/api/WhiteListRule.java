package org.example.dcheck.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.springframework.cglib.beans.BeanMap;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: 2025/3/19
 * the implementations define how to adjust relevancy search result
 *
 * @author 三石而立Sunsy
 */
public interface WhiteListRule {

    /**
     * identity
     */
    @NotNull
    String getId();

    /**
     * type hint
     */
    @NotNull
    WhiteListRuleType getType();

    /**
     * The larger the value, the more likely the respective paragraph to be ignored
     */
    @NotNull List<@NotNull DuplicatePart> calculateFilterScore(@NotNull List<@NotNull DuplicatePart> paragraphs, FilterContext handler);

    default LinkedHashMap<String, PojoField> getState() {
        if (this instanceof Map<?, ?>) {
            throw new UnsupportedOperationException("The rule is a map");
        }
        LinkedHashMap<String, PojoField> state = new LinkedHashMap<>();
        BeanMap beanMap = BeanMap.create(this);
        for (Object kvLike : beanMap.entrySet()) {
            if (!(kvLike instanceof Map.Entry)) continue;
            Map.Entry<?, ?> kv = (Map.Entry<?, ?>) kvLike;
            if (!(kv.getKey() instanceof CharSequence)) continue;
            String fieldKey = ((CharSequence) kv.getKey()).toString();
            if (kv.getValue() instanceof Serializable || kv.getValue() == null) {
                state.put(fieldKey, new PojoField(fieldKey, beanMap.getPropertyType(fieldKey), (Serializable) kv.getValue()));
            }
        }
        return state;
    }

    default void restoreState(Map<String, PojoField> state) {
        BeanMap.create(this).putAll(state);
    }


    /**
     * store the result in filtering procedure {@link #calculateFilterScore} and
     * notify the procedure whether the score represent the duplicated paragraph is capable to be filtered
     */
    interface FilterContext {

        Check getCheck();

        boolean isFiltered(@Range(from = 0, to = 1) double score);

    }
}
