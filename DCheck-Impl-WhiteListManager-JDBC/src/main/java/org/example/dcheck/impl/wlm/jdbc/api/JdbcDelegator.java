package org.example.dcheck.impl.wlm.jdbc.api;

import org.example.dcheck.annotation.Index;
import org.example.dcheck.api.AllowListRule;
import org.example.dcheck.api.AllowListRuleType;
import org.example.dcheck.api.BeanProperty;
import org.example.dcheck.api.EntityProvider;
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
public interface JdbcDelegator {

    boolean support(JdbcAgent agent, Properties jdbcProperties);

    Map<AllowListRuleType, PreparedStatement> generateMergeRuleEntity(JdbcAgent agent, Map<AllowListRuleType, List<MappedRuleEntity>> rules) throws JdbcException;

    PreparedStatement generateMergeEntityType(JdbcAgent agent, List<AllowListRule> rules) throws JdbcException;

    void executeCreateTable(JdbcAgent agent, TableCreationContext creationContext) throws JdbcException;


    interface MappedRuleEntity {
        AllowListRule getRule();

        Map<String, Serializable> getMappedValues();
    }

    interface TableCreationContext {
        String getTableName();

        EntityProvider<?> getEntityProvider();

        Map<BeanProperty, Index.Value> getIndexes();
    }
}
