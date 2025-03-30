package org.example.dcheck.impl.wlm.jdbc.core;

import org.example.dcheck.api.DCheckConfig;
import org.example.dcheck.api.WhiteListManager;
import org.example.dcheck.api.WhiteListRuleSet;
import org.example.dcheck.impl.wlm.jdbc.exception.JdbcException;
import org.example.dcheck.impl.wlm.jdbc.support.JdbcAgent;
import org.example.dcheck.spi.DCheckConfigProvider;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
public class JdbcWhiteListManager implements WhiteListManager {

    protected JdbcAgent jdbcAgent;

    @Override
    public void init() throws Exception {
        jdbcAgent = buildJdbcAgent();
    }

    protected JdbcAgent buildJdbcAgent() throws JdbcException {
        DCheckConfig config = DCheckConfigProvider.getInstance().getDCheckConfig();

        return new JdbcAgent(config.required(JdbcConfigPropertyKey.JDBC_URL), config.nullable(JdbcConfigPropertyKey.JDBC_USERNAME), config.nullable(JdbcConfigPropertyKey.JDBC_PASSWORD));
    }

    @Override
    public WhiteListRuleSet getRuleSet(String whiteListId) {
        return null;
    }

    @Override
    public void removeRuleSet(String whiteListId) {

    }

    @Override
    public void addAndSaveRuleSet(WhiteListRuleSet ruleSet) {

    }
}
