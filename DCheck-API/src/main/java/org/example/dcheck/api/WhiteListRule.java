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
@SuppressWarnings("unused")
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
    float @Range(from = 0, to = 1) [] calculateFilterScore(List<? extends @NotNull Paragraph> paragraphs);
}
