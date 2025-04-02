package org.example.dcheck.impl.alm.jdbc.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.dcheck.api.AddRuleContext;
import org.example.dcheck.api.AllowListRule;
import org.example.dcheck.api.AllowListRuleSet;
import org.example.dcheck.impl.alm.jdbc.entity.RuleSetElementEntity;
import org.example.dcheck.impl.alm.jdbc.entity.RuleSetEntity;
import org.example.dcheck.impl.alm.jdbc.exception.JdbcException;
import org.example.dcheck.impl.alm.jdbc.exception.JdbcExceptionWrapper;
import org.example.dcheck.impl.alm.jdbc.support.JdbcAgent;
import org.example.dcheck.util.CollectionUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    public void disableRule(List<String> ruleIds) {
        try {
            Map<String, RuleSetElementEntity> ruleSetElementEntityMap = agent.selectRuleSetElement(id, ruleIds);
            for (String ruleId : ruleIds) {
                RuleSetElementEntity elementEntity = ruleSetElementEntityMap.get(ruleId);
                if (elementEntity == null) continue;
                elementEntity.setEnabled(Boolean.FALSE);
            }
            agent.mergeRuleSetElement(new ArrayList<>(ruleSetElementEntityMap.values()));
        } catch (JdbcException e) {
            throw new JdbcExceptionWrapper(e);
        }
    }

    @Override
    public void removeRule(List<String> ruleIds) {
        try {
            agent.deleteElementInRuleSet(id, ruleIds);
        } catch (JdbcException e) {
            throw new JdbcExceptionWrapper(e);
        }
    }

    @Override
    public void addRule(List<AddRuleContext> contexts) {
        if (contexts.isEmpty()) return;
        try {
            agent.mergeRule(contexts.stream().map(AddRuleContext::getRule).collect(Collectors.toList()));
            agent.mergeRuleSetElement(contexts.stream().map(ctx -> {
                RuleSetElementEntity entity = new RuleSetElementEntity(id, ctx.getRule().getId());
                entity.setEnabled(ctx.isEnabled());
                entity.setOrder(ctx.getOrder());
                return entity;
            }).collect(Collectors.toList()));
        } catch (JdbcException e) {
            throw new JdbcExceptionWrapper(e);
        }
    }

    protected static final int FETCH_RULE_BATCH_SIZE = 5;

    @Override
    public Stream<? extends AllowListRule> getAllRules() {
        return getRulesImpl(() -> new ArrayList<>(agent.selectRuleSetElementByRuleSetId(id).values()));
    }

    protected interface JdbcExceptionSupplier<O> {
        O get() throws JdbcException;
    }

    protected Stream<? extends AllowListRule> getRulesImpl(JdbcExceptionSupplier<List<RuleSetElementEntity>> elementsGetter) {
        try {
            return CollectionUtils.partition(elementsGetter.get(), FETCH_RULE_BATCH_SIZE).stream()
                    .flatMap(partition -> {
                        try {
                            Map<String, AllowListRule> ruleMap = agent.selectRule(partition.stream().map(RuleSetElementEntity::getRuleId).collect(Collectors.toList()));
                            return partition.stream().map(RuleSetElementEntity::getRuleId).map(ruleMap::get);
                        } catch (JdbcException e) {
                            throw new JdbcExceptionWrapper(e);
                        }
                    });
        } catch (Throwable e) {
            if (e instanceof JdbcExceptionWrapper) {
                throw (JdbcExceptionWrapper) e;
            }
            throw new JdbcExceptionWrapper(e);
        }
    }

    @Override
    public Stream<? extends AllowListRule> getEnabledRules() {
        return getRulesImpl(() -> new ArrayList<>(agent.selectRuleSetElementByRuleSetIdAndEnabled(id, Boolean.TRUE).values()));
    }

    @Override
    public Stream<? extends AllowListRule> getDisabledRules() {
        return getRulesImpl(() -> new ArrayList<>(agent.selectRuleSetElementByRuleSetIdAndEnabled(id, Boolean.FALSE).values()));
    }

    protected RuleSetElementEntity getRuleSetElementEntity(String ruleId) {
        try {
            return agent.selectRuleSetElementByRuleSetIdAndRuleId(id, ruleId)
                    .orElseThrow(() -> new IllegalArgumentException("That rule '" + ruleId + "' isn't in rule set '" + id + "'"));
        } catch (JdbcException e) {
            throw new JdbcExceptionWrapper(e);
        }
    }

    @Override
    public int getOrder(String ruleId) {
        return getRuleSetElementEntity(ruleId).getOrder();
    }

    @Override
    public void setOrder(String ruleId, int order) {
        RuleSetElementEntity entity = getRuleSetElementEntity(ruleId);
        entity.setOrder(order);
        try {
            agent.mergeRuleSetElement(Collections.singletonList(entity));
        } catch (JdbcException e) {
            throw new JdbcExceptionWrapper(e);
        }
    }

    @Override
    public void setDescription(@Nullable String description) {
        try {
            Map<String, RuleSetEntity> result = agent.selectRuleSet(Collections.singletonList(id));
            RuleSetEntity toSave;
            RuleSetEntity selfEntity = result.get(id);
            if (selfEntity == null) {
                RuleSetEntity newSelf = new RuleSetEntity();
                newSelf.setId(id);
                newSelf.setAdded(false);
                newSelf.setDescription(description);
                toSave = newSelf;
            } else {
                selfEntity.setDescription(description);
                toSave = selfEntity;
            }
            agent.mergeRuleSet(Collections.singletonList(toSave));
        } catch (JdbcException e) {
            throw new JdbcExceptionWrapper(e);
        }
    }

    @Override
    public @Nullable String getDescription() {
        try {
            Map<String, RuleSetEntity> result = agent.selectRuleSet(Collections.singletonList(id));
            RuleSetEntity entity = result.get(id);
            if (entity == null) return null;
            return entity.getDescription();
        } catch (JdbcException e) {
            throw new JdbcExceptionWrapper(e);
        }
    }
}
