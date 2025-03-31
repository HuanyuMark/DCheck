package org.example.dcheck.api;

import lombok.experimental.ExtensionMethod;
import org.example.dcheck.annotation.Ignore;
import org.example.dcheck.util.BeanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: 2025/3/19
 * the implementations define how to adjust relevancy search result
 *
 * @author 三石而立Sunsy
 */
@ExtensionMethod({BeanUtils.class, Collections.class})
public interface WhiteListRule {

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
    WhiteListRuleType getType();

    /**
     * The larger the value, the more likely the respective paragraph to be ignored
     */
    @NotNull List<@NotNull DuplicatePart> calculateFilterScore(@NotNull List<@NotNull DuplicatePart> paragraphs, FilterContext handler);


    /**
     * get state schema.
     */
    default List<BeanProperty> getSchema() {
        return EntityProvider.getDefaultSchema(getClass());
    }


    default Map<String, PojoField> getState() {
        LinkedHashMap<String, PojoField> state = new LinkedHashMap<>();

        getSchema().forEach(p -> {
            assert p.getGetter() != null;
            state.put(p.getName(), new PojoField(p, (Serializable) p.get(this)));
        });

        return state.unmodifiableMap();
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
