package org.example.dcheck.impl.wlm.jdbc.sql;

import org.example.dcheck.api.WhiteListRule;
import org.example.dcheck.api.WhiteListRuleType;
import org.example.dcheck.common.util.MessageFormat;
import org.example.dcheck.impl.wlm.jdbc.api.BatchInsertOrUpdateSQLGenerator;
import org.example.dcheck.impl.wlm.jdbc.exception.JdbcException;
import org.example.dcheck.impl.wlm.jdbc.support.JdbcAgent;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
public class MySqlBatchInsertOrUpdateSQLGenerator implements BatchInsertOrUpdateSQLGenerator {
    @Override
    public boolean support(JdbcAgent agent, Properties jdbcProperties) {
        String url = Objects.requireNonNull(jdbcProperties.getProperty("url"));
        return url.startsWith("jdbc:mysql");
    }

    @Override
    public Map<WhiteListRuleType, PreparedStatement> generateMergeRuleEntity(JdbcAgent agent, Map<WhiteListRuleType, List<MappedRuleEntity>> rules) throws JdbcException {
        if (rules.isEmpty()) return Collections.emptyMap();
        try {
            return rules.entrySet()
                    .stream()
                    .filter(kv -> !kv.getValue().isEmpty())
                    .map(kv -> {
                        List<Map<String, Serializable>> mappedValues = kv.getValue().stream().map(MappedRuleEntity::getMappedValues).collect(Collectors.toList());
                        Map<String, Serializable> firstEntity = mappedValues.get(0);
                        WhiteListRule rule = kv.getValue().get(0).getRule();
                        try {
                            PreparedStatement stm = agent.prepareStatement(MessageFormat.format("INSERT INTO {tableName} ({keys})" +
                                    " VALUES {values}" +
                                    " ON DUPLICATE KEY UPDATE {updates};", new HashMap<String, Object>() {{

                                put("tableName", agent.inferRuleTableName(rule.getType()));
                                put("keys", String.join(",", firstEntity.keySet()));
                                put("values", mappedValues.stream().map(values -> "(" + values.values().stream().map(v -> v == null ? "NULL" : "?").collect(Collectors.joining(",")) + ")").collect(Collectors.joining(",")));
                                put("updates", firstEntity.keySet().stream().map(k -> k + "=VALUES(" + k + ")").collect(Collectors.joining(",")));

                            }}));
                            int size = mappedValues.size();
                            for (int i = 0; i < size; i++) {
                                Object[] values = mappedValues.get(i).values().stream().filter(Objects::nonNull).toArray();
                                for (int j = 0; j < values.length; j++) {
                                    stm.setObject(i + j, values[j]);
                                }
                            }
                            return new AbstractMap.SimpleEntry<>(kv.getKey(), stm);
                        } catch (JdbcException e) {
                            throw new RuntimeException(e);
                        } catch (SQLException e) {
                            throw new RuntimeException(new JdbcException("prepare statement fail: " + e.getMessage(), e));
                        }
                    }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (Throwable e) {
            if (e.getCause() instanceof JdbcException) {
                throw (JdbcException) e.getCause();
            }
            throw e;
        }

    }

    @Override
    public PreparedStatement generateMergeEntityType(JdbcAgent agent, List<WhiteListRule> rules) throws JdbcException {

        PreparedStatement stm = agent.prepareStatement(MessageFormat.format("INSERT INTO {tableName} (id, ruleType)"
                + " VALUES {values}"
                + "ON DUPLICATE KEY UPDATE ruleType=VALUES(ruleType);", new HashMap<String, Object>() {{

            put("tableName", JdbcAgent.RuleTypeEntity.tableName);
            put("values", rules.stream().map(rule -> "(" + rule.getId() + "," + rule.getType().name() + ")").collect(Collectors.joining(",")));

        }}));

        int size = rules.size();
        for (int i = 0; i < size; i += 2) {
            try {
                stm.setString(i, rules.get(i).getId());
                stm.setString(i + 1, rules.get(i).getType().name());
            } catch (SQLException e) {
                throw new JdbcException("prepare statement fail: " + e.getMessage(), e);
            }
        }
        return stm;
    }
}
