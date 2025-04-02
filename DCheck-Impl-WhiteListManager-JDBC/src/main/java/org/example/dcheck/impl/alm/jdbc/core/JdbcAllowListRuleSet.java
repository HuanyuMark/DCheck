package org.example.dcheck.impl.alm.jdbc.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.dcheck.api.AllowListRule;
import org.example.dcheck.api.AllowListRuleSet;
import org.example.dcheck.impl.alm.jdbc.entity.RuleSetElementEntity;
import org.example.dcheck.impl.alm.jdbc.exception.JdbcException;
import org.example.dcheck.impl.alm.jdbc.exception.JdbcExceptionWrapper;
import org.example.dcheck.impl.alm.jdbc.support.JdbcAgent;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Date 2025/03/31
 *
 * @author 三石而立Sunsy
 */
@RequiredArgsConstructor
public class JdbcAllowListRuleSet implements AllowListRuleSet {

    @Getter
    private final String id;

    private final JdbcAgent agent;

    @Override
    public boolean isEnabled(String ruleId) {
        try {
            Map<String, RuleSetElementEntity> ruleSetElementEntityMap = agent.selectRuleSetElement(id, Collections.singletonList(ruleId));
            RuleSetElementEntity elementEntity = ruleSetElementEntityMap.get(ruleId);
            if (elementEntity == null) return false;
            return elementEntity.getEnabled();
        } catch (JdbcException e) {
            throw new JdbcExceptionWrapper(e);
        }
    }

    @Override
    public boolean hasRule(String ruleId) {
        try {
            Map<String, RuleSetElementEntity> ruleSetElementEntityMap = agent.selectRuleSetElement(id, Collections.singletonList(ruleId));
            RuleSetElementEntity elementEntity = ruleSetElementEntityMap.get(ruleId);
            return elementEntity != null;
        } catch (JdbcException e) {
            throw new JdbcExceptionWrapper(e);
        }
    }

    @Override
    public void disableRule(String ruleId) {
        try {
            Map<String, RuleSetElementEntity> ruleSetElementEntityMap = agent.selectRuleSetElement(id, Collections.singletonList(ruleId));
            RuleSetElementEntity elementEntity = ruleSetElementEntityMap.get(ruleId);
            if (elementEntity == null) return;
            elementEntity.setEnabled(Boolean.FALSE);
            agent.mergeRuleSetElement(Collections.singletonList(elementEntity));
        } catch (JdbcException e) {
            throw new JdbcExceptionWrapper(e);
        }
    }

    @Override
    public void removeRule(String ruleId) {
        try {
            agent.deleteElementInRuleSet(id, Collections.singletonList(ruleId));
        } catch (JdbcException e) {
            throw new JdbcExceptionWrapper(e);
        }
    }

    @Override
    public void addRule(AllowListRule rule, boolean enabled) {
        try {
            Map<String, RuleSetElementEntity> ruleSetElementEntityMap = agent.selectRuleSetElement(id, Collections.singletonList(rule.getId()));
            RuleSetElementEntity elementEntity = ruleSetElementEntityMap.get(rule.getId());
            agent.mergeRuleSetElement(id, Collections.singletonList(ruleId));
        } catch (JdbcException e) {
            throw new JdbcExceptionWrapper(e);
        }
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
