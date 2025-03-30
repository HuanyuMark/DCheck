package org.example.dcheck.impl.wlm.jdbc.api;

import org.example.dcheck.api.WhiteListRule;
import org.example.dcheck.api.WhiteListRuleType;
import org.example.dcheck.impl.wlm.jdbc.exception.JdbcException;
import org.example.dcheck.impl.wlm.jdbc.support.JdbcAgent;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
public interface BatchInsertOrUpdateSQLGenerator {

    boolean support(JdbcAgent agent, Properties jdbcProperties);

    Map<WhiteListRuleType, PreparedStatement> generateMergeRuleEntity(JdbcAgent agent, Map<WhiteListRuleType, List<MappedRuleEntity>> rules) throws JdbcException;

    PreparedStatement generateMergeEntityType(JdbcAgent agent, List<WhiteListRule> rules) throws JdbcException;


    interface MappedRuleEntity {
        WhiteListRule getRule();

        Map<String, Serializable> getMappedValues();
    }
}
