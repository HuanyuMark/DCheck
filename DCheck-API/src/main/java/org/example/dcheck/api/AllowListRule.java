package org.example.dcheck.api;

import lombok.experimental.ExtensionMethod;
import org.example.dcheck.annotation.Ignore;
import org.example.dcheck.util.BeanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.Collections;
import java.util.List;

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
