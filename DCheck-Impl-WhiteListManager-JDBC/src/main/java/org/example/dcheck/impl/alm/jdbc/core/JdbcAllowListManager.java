package org.example.dcheck.impl.alm.jdbc.core;

import org.example.dcheck.api.AllowListManager;
import org.example.dcheck.api.AllowListRuleSet;
import org.example.dcheck.impl.alm.jdbc.api.JdbcDataSourceProvider;
import org.example.dcheck.impl.alm.jdbc.entity.RuleSetEntity;
import org.example.dcheck.impl.alm.jdbc.exception.JdbcException;
import org.example.dcheck.impl.alm.jdbc.exception.JdbcExceptionWrapper;
import org.example.dcheck.impl.alm.jdbc.support.JdbcAgent;
import org.example.dcheck.spi.JdbcDataSourceProviderProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
public class JdbcAllowListManager implements AllowListManager {

    protected JdbcAgent agent;

    @Override
    public void init() throws Exception {
        agent = buildJdbcAgent();
    }

    protected JdbcAgent buildJdbcAgent() throws JdbcException {
        JdbcDataSourceProvider provider = JdbcDataSourceProviderProvider.getInstance().getProvider();
        return new JdbcAgent(provider.getDataSourceInfo(), provider.getDataSource());
    }

    @Override
    public AllowListRuleSet getRuleSet(String ruleSetId) {
        Map<String, RuleSetEntity> entity;
        try {
            entity = agent.selectRuleSet(Collections.singletonList(ruleSetId));
        } catch (JdbcException e) {
            throw new JdbcExceptionWrapper(e);
        }
        RuleSetEntity result = entity.get(ruleSetId);
        if (result == null) {
            return null;
        }
        return new JdbcAllowListRuleSet(ruleSetId, agent);
    }

    @Override
    public @NotNull AllowListRuleSet getOrCreateRuleSet(String ruleSetId) {
        try {
            Map<String, RuleSetEntity> entity = agent.selectRuleSet(Collections.singletonList(ruleSetId));
            RuleSetEntity result = entity.get(ruleSetId);
            if (result != null) {
                return new JdbcAllowListRuleSet(ruleSetId, agent);
            }
        } catch (JdbcException e) {
            throw new JdbcExceptionWrapper(e);
        }
        try {
            agent.mergeRuleSet(Collections.singletonList(new RuleSetEntity(ruleSetId, null, Boolean.FALSE)));
        } catch (JdbcException e) {
            throw new JdbcExceptionWrapper(e);
        }
        return new JdbcAllowListRuleSet(ruleSetId, agent);
    }

    @Override
    public void removeRuleSet(String ruleSetId) {
        try {
            agent.deleteElementInRuleSet(ruleSetId);
        } catch (JdbcException e) {
            throw new JdbcExceptionWrapper(e);
        }
    }

    @Override
    public @NotNull List<AllowListRuleSet> getAllRuleSets() {
        return Collections.emptyList();
    }
}
