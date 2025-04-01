package org.example.dcheck.impl.alm.jdbc.api;

import org.example.dcheck.api.AllowListRule;
import org.example.dcheck.api.AllowListRuleType;
import org.example.dcheck.api.EntityProvider;
import org.example.dcheck.impl.alm.jdbc.annotation.Index;
import org.example.dcheck.impl.alm.jdbc.exception.JdbcException;
import org.example.dcheck.impl.alm.jdbc.support.JdbcAgent;
import org.example.dcheck.util.BeanProperty;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
public interface JdbcDelegator {

    boolean support(JdbcAgent agent);

    Map<AllowListRuleType, PreparedStatement> generateMergeRuleEntity(JdbcAgent agent, Map<AllowListRuleType, List<MappedRuleEntity>> rules) throws JdbcException;

    PreparedStatement generateMergeEntityType(JdbcAgent agent, List<AllowListRule> rules) throws JdbcException;

    void executeCreateTable(JdbcAgent agent, TableCreationContext creationContext) throws JdbcException;

    void executeMergeRuleSetEntity(JdbcAgent agent, List<JdbcAgent.RuleSetEntity> entities) throws JdbcException;

    void executeMergeRuleSetElement(JdbcAgent jdbcAgent, List<MappedRuleSetElementEntity> entities) throws JdbcException;


    interface MappedRuleEntity {
        AllowListRule getRule();

        Map<String, Serializable> getMappedValues();
    }

    interface MappedRuleSetElementEntity {
        JdbcAgent.RuleSetElementEntity getEntity();

        Map<String, Serializable> getMappedValues();
    }

    interface TableCreationContext {
        String getTableName();

        EntityProvider<?> getEntityProvider();

        Map<BeanProperty, Index.Value> getIndexes();
    }
}
