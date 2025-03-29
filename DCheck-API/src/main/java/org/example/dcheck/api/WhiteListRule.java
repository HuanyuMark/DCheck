package org.example.dcheck.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.List;

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

    /**
     * store the result in filtering procedure {@link #calculateFilterScore} and
     * notify the procedure whether the score represent the duplicated paragraph is capable to be filtered
     */
    interface FilterContext {

        Check getCheck();

        boolean isFiltered(@Range(from = 0, to = 1) double score);

    }
}
