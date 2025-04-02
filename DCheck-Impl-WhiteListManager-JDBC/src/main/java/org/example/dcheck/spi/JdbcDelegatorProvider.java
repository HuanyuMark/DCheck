package org.example.dcheck.spi;

import org.example.dcheck.impl.alm.jdbc.api.JdbcDelegator;
import org.example.dcheck.impl.alm.jdbc.exception.UnsupportedBatchOperationException;
import org.example.dcheck.impl.alm.jdbc.support.JdbcAgent;

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

    protected JdbcDelegatorProvider() {
    }

    public JdbcDelegator find(JdbcAgent agent) {
        return Providers.findAllImplementations(JdbcDelegator.class)
                .stream()
                .filter(g -> g.support(agent))
                .findFirst()
                .orElseThrow(() -> new UnsupportedBatchOperationException("Unsupported Batch Operation for " + agent.getDataSourceInfo()));
    }
}
