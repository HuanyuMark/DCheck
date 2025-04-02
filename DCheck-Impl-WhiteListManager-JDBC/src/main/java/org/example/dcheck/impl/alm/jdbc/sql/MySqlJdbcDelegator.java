package org.example.dcheck.impl.alm.jdbc.sql;

import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.annotation.Ignore;
import org.example.dcheck.api.AllowListRule;
import org.example.dcheck.api.AllowListRuleType;
import org.example.dcheck.api.EntityProvider;
import org.example.dcheck.api.PojoField;
import org.example.dcheck.common.util.MessageFormat;
import org.example.dcheck.impl.alm.jdbc.annotation.Index;
import org.example.dcheck.impl.alm.jdbc.api.EntityFieldMapper;
import org.example.dcheck.impl.alm.jdbc.api.JdbcDelegator;
import org.example.dcheck.impl.alm.jdbc.entity.RuleSetElementEntity;
import org.example.dcheck.impl.alm.jdbc.entity.RuleSetEntity;
import org.example.dcheck.impl.alm.jdbc.entity.RuleTypeEntity;
import org.example.dcheck.impl.alm.jdbc.exception.JdbcException;
import org.example.dcheck.impl.alm.jdbc.exception.UnsupportedFieldTypeException;
import org.example.dcheck.impl.alm.jdbc.support.JdbcAgent;
import org.example.dcheck.spi.RuleEntityFieldMapperProvider;
import org.example.dcheck.util.BeanProperty;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
@Slf4j
public class MySqlJdbcDelegator implements JdbcDelegator {
    @Override
    public boolean support(JdbcAgent agent) {
        return agent.getDataSourceInfo().getDatabaseType().contains("mysql");
    }

    @Override
    public Map<AllowListRuleType, PreparedStatement> generateMergeRuleEntity(JdbcAgent agent, Map<AllowListRuleType, List<MappedRuleEntity>> rules) throws JdbcException {
        if (rules.isEmpty()) return Collections.emptyMap();
        try {
            return rules.entrySet()
                    .stream()
                    .filter(kv -> !kv.getValue().isEmpty())
                    .map(kv -> {
                        List<Map<String, Object>> mappedValues = kv.getValue().stream().map(MappedRuleEntity::getMappedValues).collect(Collectors.toList());
                        Map<String, Object> firstEntity = mappedValues.get(0);
                        AllowListRule rule = kv.getValue().get(0).getRule();
                        try {
                            PreparedStatement stm = agent.prepareStatement(MessageFormat.format("INSERT INTO {tableName} ({keys})" +
                                    " VALUES {values}" +
                                    " ON DUPLICATE KEY UPDATE {updates};", new HashMap<String, Object>() {{

                                put("tableName", agent.inferRuleTableName(rule.getType()));
                                put("keys", String.join(",", firstEntity.keySet()));
                                put("values", mappedValues.stream().map(values -> "(" + values.values().stream().map(v -> v == null ? "NULL" : "?").collect(Collectors.joining(",")) + ")").collect(Collectors.joining(",")));
                                put("updates", firstEntity.keySet().stream().map(k -> k + "=VALUES(" + k + ")").collect(Collectors.joining(",")));

                            }}));
                            stm.closeOnCompletion();
                            setMappedValuesInStm(mappedValues, stm);
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
    public PreparedStatement generateMergeEntityType(JdbcAgent agent, List<AllowListRule> rules) throws JdbcException {

        PreparedStatement stm = agent.prepareStatement(MessageFormat.format("INSERT INTO {tableName} (id, ruleType)"
                + " VALUES {values}"
                + "ON DUPLICATE KEY UPDATE ruleType=VALUES(ruleType);", new HashMap<String, Object>() {{

            put("tableName", RuleTypeEntity.tableName);
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

    @Override
    public void executeCreateTable(JdbcAgent agent, TableCreationContext creationContext) throws JdbcException {
        checkIndex(creationContext.getIndexes().entrySet().stream().collect(Collectors.groupingBy(e -> e.getValue().getValue())));

        List<EntityFieldMapper> mappers = RuleEntityFieldMapperProvider.getInstance().getMappers();

        Map<BeanProperty, String> jdbcFieldTypes = creationContext.getEntityProvider()
                .getSchema()
                .stream()
                .filter(p -> !p.isGetterAnnPresent(Ignore.class))
                .map(property -> {
                    EntityFieldMapper mapper = mappers.stream().filter(m -> m.support(agent, creationContext.getEntityProvider(), new PojoField(property, null))).findFirst()
                            .orElseThrow(() -> new UnsupportedFieldTypeException("Unsupported Property '" + property + "'."));
                    String jdbcFieldType = mapper.getJdbcFieldType(agent, creationContext.getEntityProvider(), new PojoField(property, null));
                    return new AbstractMap.SimpleEntry<>(property, jdbcFieldType);
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (u, v) -> {
                    throw new IllegalStateException(String.format("Duplicate key %s", u));
                }, LinkedHashMap::new));


        Stream<String> indexStatements = createIndexStatements(creationContext, jdbcFieldTypes);

        Map<String, Object> ctx = new HashMap<>(4);
        ctx.put("tableSchema", Stream.concat(jdbcFieldTypes.entrySet().stream().map(p -> p.getKey().getName() + " " + p.getValue()), indexStatements).collect(Collectors.joining(",")));
        ctx.put("tableName", creationContext.getTableName());
        String sql = MessageFormat.format("CREATE TABLE IF NOT EXIST {tableName} ({tableSchema});", ctx);
        try (Connection con = agent.getConnection(); Statement stm = con.prepareStatement(sql)) {
            JdbcAgent.logSql(stm);
            stm.execute(sql);
        } catch (SQLException e) {
            throw new JdbcException("close connection fail: " + e.getMessage(), e);
        }
    }

    protected final static String RULE_SET_ENTITY_MERGE_STM = RuleSetEntity.provider.getSchema().stream().map(p -> p.getName() + "=VALUES(" + p.getName() + ")").collect(Collectors.joining(","));

    protected static final String RULE_SET_ENTITY_COLUMNS = RuleSetElementEntity.provider.getSchema().stream().map(BeanProperty::getName).collect(Collectors.joining(","));


    @Override
    public void executeMergeRuleSetEntity(JdbcAgent agent, List<MappedRuleSetEntity> entities) throws JdbcException {
        String valuesSeg = entities.stream().map(entity -> "(?,?)").collect(Collectors.joining(","));
        try (Connection con = agent.getConnection();
             PreparedStatement stm = con.prepareStatement(String.format("INSERT INTO " + RuleSetEntity.tableName + " (%s) " +
                     "VALUES %s " +
                             "ON DUPLICATE KEY UPDATE %s;",
                     RULE_SET_ENTITY_COLUMNS,
                     valuesSeg,
                     RULE_SET_ENTITY_MERGE_STM))) {

            setMappedValuesInStm(entities.stream().map(MappedRuleSetEntity::getMappedValues).collect(Collectors.toList()), stm);

            JdbcAgent.logSql(stm);

            stm.execute();
        } catch (SQLException e) {
            throw new JdbcException("execute MergeRuleSetEntity fail: ", e);
        }
    }

    protected static final String RULE_SET_ELEMENT_ENTITY_COLUMNS = RuleSetElementEntity.provider.getSchema().stream().map(BeanProperty::getName).collect(Collectors.joining(","));

    protected static final String RULE_SET_ELEMENT_UPDATE_STM = RuleSetElementEntity.provider.getSchema().stream().map(p -> p.getName() + "=VALUES(" + p.getName() + ")").collect(Collectors.joining(","));

    @Override
    public void executeMergeRuleSetElement(JdbcAgent agent, List<MappedRuleSetElementEntity> entities) throws JdbcException {
        List<Map<String, Object>> mappedValues = entities.stream().map(MappedRuleSetElementEntity::getMappedValues).collect(Collectors.toList());
        String valuesSeg = mappedValues.stream().map(values -> "(" + values.values().stream().map(v -> v == null ? "NULL" : "?").collect(Collectors.joining(",")) + ")").collect(Collectors.joining(","));

        try (Connection con = agent.getConnection();
             PreparedStatement stm = con.prepareStatement(String.format("INSERT INTO " + RuleSetElementEntity.tableName + " (%s) " +
                             "VALUES %s " +
                             "ON DUPLICATE KEY UPDATE %s;",
                     RULE_SET_ELEMENT_ENTITY_COLUMNS,
                     valuesSeg,
                     RULE_SET_ELEMENT_UPDATE_STM
             ))) {

            setMappedValuesInStm(mappedValues, stm);

            JdbcAgent.logSql(stm);

            stm.execute();
        } catch (SQLException e) {
            throw new JdbcException("Execute MergeRuleSetEntity fail: ", e);
        }
    }

    private void setMappedValuesInStm(List<Map<String, Object>> mappedValues, PreparedStatement stm) throws SQLException {
        int size = mappedValues.size();
        for (int i = 0; i < size; i++) {
            Object[] values = mappedValues.get(i).values().stream().filter(Objects::nonNull).toArray();
            for (int j = 0; j < values.length; j++) {
                stm.setObject(i + j, values[j]);
            }
        }
    }

    @NotNull
    private Stream<String> createIndexStatements(TableCreationContext creationContext, Map<BeanProperty, String> jdbcFieldTypes) {
        return creationContext.getIndexes()
                .entrySet()
                .stream()
                .filter(e -> {
                    if (!jdbcFieldTypes.containsKey(e.getKey())) {
                        log.warn("Ignore index of property '{}.{}' because of @Ignore", e.getKey().getBeanType(), e.getKey().getName());
                        return false;
                    }
                    return true;
                })
                .map(e -> {
                    String indexStm;
                    switch (e.getValue().getValue()) {
                        case NORMAL:
                            indexStm = "INDEX {indexName} ({fieldName})";
                            break;
                        case UNIQUE:
                            indexStm = "UNIQUE INDEX {indexName} ({fieldName})";
                            break;
                        case PRIMARY:
                            indexStm = "PRIMARY KEY ({fieldName})";
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + e.getValue());
                    }
                    Map<String, Object> indexCtx = new HashMap<>(3);
                    indexCtx.put("indexName", createIndexName(creationContext.getEntityProvider(), e.getKey(), e.getValue()));
                    indexCtx.put("fieldName", e.getKey().getName());
                    return MessageFormat.format(indexStm, indexCtx);
                });
    }

    protected void checkIndex(Map<Index.IndexType, List<Map.Entry<BeanProperty, Index.Value>>> duplicateCheck) throws JdbcException {
        List<Map.Entry<BeanProperty, Index.Value>> primaryIndexed = duplicateCheck.get(Index.IndexType.PRIMARY);
        if (primaryIndexed != null && primaryIndexed.size() > 1) {
            throw new JdbcException("Duplicate primary indexed: " + primaryIndexed.stream().map(Map.Entry::getKey).map(BeanProperty::getName).collect(Collectors.joining(",")));
        }
    }

    protected String createIndexName(EntityProvider<?> entityProvider, BeanProperty property, Index.Value index) {
        return JdbcAgent.DCHECK_TABLE_PREFIX + "idx_" + escapeNameToSql(entityProvider.getType().getSimpleName()) + "_" + index.getValue() + "_" + escapeNameToSql(property.getName());
    }

    protected String escapeNameToSql(String str) {
        return str.replaceAll("[$.|*+-]", "_");
    }
}
