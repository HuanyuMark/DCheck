package org.example.dcheck.api;

import lombok.experimental.ExtensionMethod;
import org.example.dcheck.annotation.Ignore;
import org.example.dcheck.util.BeanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Date: 2025/3/19
 * the implementations define how to adjust relevancy search result
 *
 * @author 三石而立Sunsy
 */
@ExtensionMethod({BeanUtils.class, Collections.class})
public interface AllowListRule {

    /**
     * identity
     */
    @NotNull
    String getId();

    /**
     * description
     */
    @Nullable
    String getDescription();

    /**
     * type hint
     */
    @Ignore
    @NotNull
    AllowListRuleType getType();

    default Map<String, PojoField> getState() {
        return getType()
                .getSchema()
                .stream()
                .map(p -> new AbstractMap.SimpleEntry<>(p.getName(), new PojoField(p, (Serializable) p.get(AllowListRule.this))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> {
                    throw new IllegalStateException("duplicate state field '" + k1 + "' and '" + k2 + "'");
                }, LinkedHashMap::new))
                .unmodifiableMap();
    }

    /**
     * The larger the value, the more likely the respective paragraph to be ignored
     */
    @NotNull List<@NotNull DuplicatePart> calculateFilterScore(@NotNull List<@NotNull DuplicatePart> paragraphs, FilterContext handler);

    /**
     * store the result in filtering procedure {@link #calculateFilterScore} and
     * notify the procedure whether the score represent the duplicated paragraph is capable to be filtered
     */
    interface FilterContext {

        Check getCheck();

        boolean isFiltered(@Range(from = 0, to = 1) double score);
    }
}
