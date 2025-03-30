package org.example.dcheck.spi;

import org.example.dcheck.impl.wlm.jdbc.api.BatchInsertOrUpdateSQLGenerator;
import org.example.dcheck.impl.wlm.jdbc.exception.UnsupportedBatchOperationException;
import org.example.dcheck.impl.wlm.jdbc.support.JdbcAgent;

import java.util.Properties;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
public class BatchInsertOrUpdateSQLGeneratorProvider implements DCheckProvider {
    protected static final BatchInsertOrUpdateSQLGeneratorProvider INSTANCE = new BatchInsertOrUpdateSQLGeneratorProvider();

    public static BatchInsertOrUpdateSQLGeneratorProvider getInstance() {
        return INSTANCE;
    }

    public BatchInsertOrUpdateSQLGenerator find(JdbcAgent agent, Properties jdbcProperties) {
        return Providers.findAllImplementations(BatchInsertOrUpdateSQLGenerator.class)
                .stream()
                .filter(g -> g.support(agent, jdbcProperties))
                .findFirst()
                .orElseThrow(() -> new UnsupportedBatchOperationException("Unsupported Batch Operation for " + jdbcProperties));
    }
}
