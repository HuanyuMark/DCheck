package org.example.dcheck.impl.alm.jdbc.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.dcheck.api.AllowListRule;
import org.example.dcheck.api.AllowListRuleSet;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Date 2025/03/31
 *
 * @author 三石而立Sunsy
 */
@RequiredArgsConstructor
@Getter
public class JdbcAllowListRuleSet implements AllowListRuleSet {

    private final String id;

    @Override
    public boolean isEnabled(String ruleId) {
        return false;
    }

    @Override
    public boolean hasRule(String ruleId) {
        return false;
    }

    @Override
    public void disableRule(String ruleId) {

    }

    @Override
    public void removeRule(String ruleId) {

    }

    @Override
    public void addRule(AllowListRule rule, boolean enabled) {

    }

    @Override
    public Stream<? extends AllowListRule> getAllRules() {
        return Stream.empty();
    }

    @Override
    public Stream<? extends AllowListRule> getEnabledRules() {
        return Stream.empty();
    }

    @Override
    public Stream<? extends AllowListRule> getDisabledRules() {
        return Stream.empty();
    }

    @Override
    public int getOrder(String ruleId) {
        return 0;
    }

    @Override
    public void setOrder(String ruleId, int order) {

    }

    @Override
    public void setDescription(@Nullable String description) {
    }

    @Override
    public @Nullable String getDescription() {
        return "";
    }
}
