package org.example.dcheck.api;

import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * Date: 2025/3/17
 * TODO add support to filter that word in white list...
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface WhiteListRuleSet {

    /**
     * the identity of a collection of white list rule
     */
    @NotNull
    String getId();

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
    void disableRule(String ruleId);

    /**
     * if the rule store in the set, the rule would be disabled
     * and removed form the set
     */
    void removeRule(String ruleId);

    /**
     * if the rule store in the set, the rule would be disabled
     */
    default void disableRule(WhiteListRule rule) {
        disableRule(rule.getId());
    }

    /**
     * if call this method with same rule. it will replace the old rule
     * and {@code enabled} the last rule
     */
    void addRule(WhiteListRule rule, boolean enabled);

    /**
     * get all rule stored in the set whether enabled or disabled.
     * equals to {@link #getEnabledRules()} + {@link #getDisabledRules()}
     * order by {@link #getOrder}
     */
    Stream<? extends WhiteListRule> getAllRules();

    /**
     * get all enabled rule stored in the set
     * order by {@link #getOrder}
     */
    Stream<? extends WhiteListRule> getEnabledRules();

    /**
     * get all disabled rule stored in the set
     * order by {@link #getOrder}
     */
    Stream<? extends WhiteListRule> getDisabledRules();


    /**
     * default order is {@link org.springframework.core.Ordered#LOWEST_PRECEDENCE}
     * indicate the applied order of the container {@link WhiteListRuleSet}
     *
     * @throws IllegalArgumentException if the rule is not in the set
     */
    int getOrder(String ruleId);

    /**
     * default order is {@link org.springframework.core.Ordered#LOWEST_PRECEDENCE}
     * indicate the applied order of the container {@link WhiteListRuleSet}
     *
     * @throws IllegalArgumentException if the rule is not in the set
     */
    default int getOrder(WhiteListRule rule) {
        return getOrder(rule.getId());
    }

    /**
     * default order is {@link org.springframework.core.Ordered#LOWEST_PRECEDENCE}
     * indicate the applied order of the container {@link WhiteListRuleSet}
     *
     * @throws IllegalArgumentException if the rule is not in the set
     */
    void setOrder(String ruleId, int order);

    /**
     * default order is {@link org.springframework.core.Ordered#LOWEST_PRECEDENCE}
     * indicate the applied order of the container {@link WhiteListRuleSet}
     *
     * @throws IllegalArgumentException if the rule is not in the set
     */
    default void setOrder(WhiteListRule ruleId, int order) {
        setOrder(ruleId.getId(), order);
    }

}
