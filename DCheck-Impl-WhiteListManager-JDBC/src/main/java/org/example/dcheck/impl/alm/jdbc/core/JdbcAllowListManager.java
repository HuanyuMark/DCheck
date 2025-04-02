package org.example.dcheck.impl.alm.jdbc.core;

import org.example.dcheck.api.AllowListManager;
import org.example.dcheck.api.AllowListRuleSet;
import org.example.dcheck.impl.alm.jdbc.api.JdbcDataSourceProvider;
import org.example.dcheck.impl.alm.jdbc.exception.JdbcException;
import org.example.dcheck.impl.alm.jdbc.support.JdbcAgent;
import org.example.dcheck.spi.JdbcDataSourceProviderProvider;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
public class JdbcAllowListManager implements AllowListManager {

    protected JdbcAgent jdbcAgent;

    @Override
    public void init() throws Exception {
        jdbcAgent = buildJdbcAgent();
    }

    protected JdbcAgent buildJdbcAgent() throws JdbcException {
        JdbcDataSourceProvider provider = JdbcDataSourceProviderProvider.getInstance().getProvider();
        return new JdbcAgent(provider.getDataSourceInfo(), provider.getDataSource());
    }

    @Override
    public AllowListRuleSet getRuleSet(String ruleSetId) {
        return null;
    }

    @Override
    public void removeRuleSet(String ruleSetId) {

    }

    @Override
    public void addRuleSet(AllowListRuleSet ruleSet) {

    }
}
