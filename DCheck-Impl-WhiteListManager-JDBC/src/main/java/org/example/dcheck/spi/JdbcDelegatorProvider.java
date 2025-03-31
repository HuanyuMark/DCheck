package org.example.dcheck.spi;

import org.example.dcheck.impl.wlm.jdbc.api.JdbcDelegator;
import org.example.dcheck.impl.wlm.jdbc.exception.UnsupportedBatchOperationException;
import org.example.dcheck.impl.wlm.jdbc.support.JdbcAgent;

import java.util.Properties;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
public class JdbcDelegatorProvider implements DCheckProvider {
    protected static final JdbcDelegatorProvider INSTANCE = new JdbcDelegatorProvider();

    public static JdbcDelegatorProvider getInstance() {
        return INSTANCE;
    }

    public JdbcDelegator find(JdbcAgent agent, Properties jdbcProperties) {
        return Providers.findAllImplementations(JdbcDelegator.class)
                .stream()
                .filter(g -> g.support(agent, jdbcProperties))
                .findFirst()
                .orElseThrow(() -> new UnsupportedBatchOperationException("Unsupported Batch Operation for " + jdbcProperties));
    }
}
