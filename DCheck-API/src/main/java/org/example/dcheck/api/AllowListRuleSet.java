package org.example.dcheck.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

/**
 * Date: 2025/3/17
 * TODO add support to filter that word in white list...
 *
 * @author 三石而立Sunsy
 */

public interface AllowListRuleSet {

    /**
     * the identity to a collection of allowlist rules
     */
    @NotNull
    String getId();

    @Nullable
    String getDescription();

    void setDescription(String description);

    /**
     * if a rule is enabled, we claim that rule would be used in DuplicateChecking procedure
     * <p>
     * if the rule store in the set, return true if the rule is enabled
     * otherwise return false
     */
    boolean isEnabled(String ruleId);

    /**
     * if the rule store in the set, return true otherwise return false
     */
    boolean hasRule(String ruleId);

    /**
     * if the rule store in the set, the rule would be disabled
     */
    void disableRule(List<String> ruleIds);

    /**
     * if the rule store in the set, the rule would be disabled
     * and removed form the set
     */
    void removeRule(List<String> ruleIds);

    /**
     * if call this method with the same rule.
     * it will replace the old rule (delete old rule and add new rule)
     * and {@code enabled} the last rule
     */
    void addRule(List<AddRuleContext> contexts);

    /**
     * get all rule stored in the set whether enabled or disabled.
     * equals to {@link #getEnabledRules()} + {@link #getDisabledRules()}
     * order by {@link #getOrder}
     */
    Stream<? extends AllowListRule> getAllRules();

    /**
     * get all enabled rule stored in the set
     * order by {@link #getOrder}
     */
    Stream<? extends AllowListRule> getEnabledRules();

    /**
     * get all disabled rule stored in the set
     * order by {@link #getOrder}
     */
    Stream<? extends AllowListRule> getDisabledRules();


    /**
     * default order is {@link org.springframework.core.Ordered#LOWEST_PRECEDENCE}
     * indicate the applied order of the container {@link AllowListRuleSet}
     *
     * @throws IllegalArgumentException if the rule is not in the set
     */
    int getOrder(String ruleId);

    /**
     * default order is {@link org.springframework.core.Ordered#LOWEST_PRECEDENCE}
     * indicate the applied order of the container {@link AllowListRuleSet}
     *
     * @throws IllegalArgumentException if the rule is not in the set
     */
    default int getOrder(AllowListRule rule) {
        return getOrder(rule.getId());
    }

    /**
     * default order is {@link org.springframework.core.Ordered#LOWEST_PRECEDENCE}
     * indicate the applied order of the container {@link AllowListRuleSet}
     *
     * @throws IllegalArgumentException if the rule is not in the set
     */
    void setOrder(String ruleId, int order);

    /**
     * default order is {@link org.springframework.core.Ordered#LOWEST_PRECEDENCE}
     * indicate the applied order of the container {@link AllowListRuleSet}
     *
     * @throws IllegalArgumentException if the rule is not in the set
     */
    default void setOrder(AllowListRule ruleId, int order) {
        setOrder(ruleId.getId(), order);
    }

}
