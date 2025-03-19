package org.example.dcheck.api;

import org.jetbrains.annotations.NotNull;

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
     * and {@code #enabled} the last rule
     */
    void addRule(WhiteListRule rule, boolean enabled);

}
