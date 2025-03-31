package org.example.dcheck.impl.wlm.jdbc.support;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.annotation.Index;
import org.example.dcheck.api.*;
import org.example.dcheck.common.util.MessageFormat;
import org.example.dcheck.impl.wlm.jdbc.api.BatchInsertOrUpdateSQLGenerator;
import org.example.dcheck.impl.wlm.jdbc.api.EntityFieldMapper;
import org.example.dcheck.impl.wlm.jdbc.exception.JdbcException;
import org.example.dcheck.impl.wlm.jdbc.exception.UnsupportedFieldTypeException;
import org.example.dcheck.spi.BatchInsertOrUpdateSQLGeneratorProvider;
import org.example.dcheck.spi.DCheckConfigProvider;
import org.example.dcheck.spi.RuleEntityFieldMapperProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.NonNull;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
@Slf4j
public class JdbcAgent implements AutoCloseable {

    protected final DataSource dataSource;

    protected final ThreadLocal<Connection> connection = new ThreadLocal<>();

    protected final Properties jdbcProperties;

    protected final Set<Class<?>> entityTableGenerated = ConcurrentHashMap.newKeySet();

    protected final Map<EntityProvider<?>, Map<BeanProperty, EntityFieldMapper>> fieldMappers = new ConcurrentHashMap<>();

    protected final BatchInsertOrUpdateSQLGenerator batchInsertOrUpdateSQLGenerator;

    protected static final String DCHECK_TABLE_PREFIX = "dcheck_";

    protected static final String RULE_TABLE_PREFIX = DCHECK_TABLE_PREFIX + "wlr_state_";

    public JdbcAgent(@NonNull String url, String username, String password) throws JdbcException {
        jdbcProperties = new Properties(DCheckConfigProvider.getInstance().getDCheckConfig().getValues());
        jdbcProperties.setProperty("url", url);
        jdbcProperties.setProperty("username", username);
        jdbcProperties.setProperty("password", password);
        this.dataSource = buildDataSource();
        batchInsertOrUpdateSQLGenerator = BatchInsertOrUpdateSQLGeneratorProvider.getInstance().find(this, jdbcProperties);
        prepareBuiltinTable();
    }

    protected DataSource buildDataSource() {
        DruidDataSource druidDataSource = new DruidDataSource();
        jdbcProperties.setProperty("druid.url", jdbcProperties.getProperty("url"));
        jdbcProperties.setProperty("druid.username", jdbcProperties.getProperty("username"));
        jdbcProperties.setProperty("druid.password", jdbcProperties.getProperty("password"));
        druidDataSource.configFromPropeties(jdbcProperties);
        return druidDataSource;
    }

    @Data
    protected static class DefaultMappedRuleEntity implements BatchInsertOrUpdateSQLGenerator.MappedRuleEntity {
        protected final WhiteListRule rule;
        protected final Map<String, Serializable> mappedValues;
    }

    protected Optional<Connection> tryGetConnection() {
        return Optional.ofNullable(this.connection.get());
    }

    protected Connection getConnection() throws JdbcException {
        Connection connection = this.connection.get();
        if (connection != null) {
            return connection;
        }
        try {
            Connection newC = new ConnectionProxy(dataSource.getConnection());
            this.connection.set(newC);
            return newC;
        } catch (SQLException e) {
            throw new JdbcException("get connection from dataSource fail: " + e.getMessage(), e);
        }
    }

    public PreparedStatement prepareStatement(@NonNull String sql) throws JdbcException {
        try {
            return getConnection().prepareStatement(sql);
        } catch (SQLException e) {
            throw new JdbcException("create prepared sql fail: " + e.getMessage(), e);
        }
    }

    public void insertOrUpdate(List<WhiteListRule> rules) throws JdbcException {
        if (rules.isEmpty()) return;
        createTableIfNeeded(rules.get(0));
        Map<WhiteListRuleType, List<BatchInsertOrUpdateSQLGenerator.MappedRuleEntity>> mappedGroups = rules.stream().map(rule -> {
            Map<String, PojoField> state = rule.getState();
            Map<BeanProperty, EntityFieldMapper> mappers = getMappers(rule.getType());

            Map<String, Serializable> mapped = new LinkedHashMap<>(state.size(), 1);
            for (Map.Entry<String, PojoField> entry : state.entrySet()) {
                mapped.put(entry.getKey(), mappers.get(entry.getValue().getProperty()).mapToJdbcFieldValue(jdbcProperties, rule.getType(), entry));
            }

            return new DefaultMappedRuleEntity(rule, mapped);
        }).collect(Collectors.groupingBy(e -> e.getRule().getType()));

        executeInTransaction("insert/update", () -> {
            Map<WhiteListRuleType, PreparedStatement> stm = batchInsertOrUpdateSQLGenerator.generateMergeRuleEntity(this, mappedGroups);
            for (PreparedStatement sql : stm.values()) {
                try (PreparedStatement ignored = sql) {
                    logSql(sql);
                    sql.execute();
                }
            }
            try (PreparedStatement storeTypeSql = batchInsertOrUpdateSQLGenerator.generateMergeEntityType(this, rules)) {
                logSql(storeTypeSql);
                storeTypeSql.execute();
            }
        });
    }

//    protected Map<WhiteListRuleType, >

    public void remove(Set<@NotNull String> ruleIds) throws JdbcException {
        if (ruleIds.isEmpty()) return;
        executeInTransaction("remove", () -> {
            try (Connection con = getConnection()) {
                Map<WhiteListRuleType, List<RuleTypeEntity>> types = getRuleTypes(con, ruleIds);
                List<PreparedStatement> deleteSql = types.entrySet()
                        .stream()
                        .map(group -> {
                            try {
                                PreparedStatement removeSql = con.prepareStatement(MessageFormat.format("DELETE FROM {tableName} WHERE id IN ({ids})", new HashMap<String, Object>() {{
                                    put("tableName", inferRuleTableName(group.getKey()));
                                    put("ids", group.getValue().stream().map(r -> "?").collect(Collectors.joining(",")));
                                }}));
                                int size = group.getValue().size();
                                for (int i = 0; i < size; i++) {
                                    logSql(removeSql);
                                    removeSql.setString(i, group.getValue().get(i).getId());
                                }
                                return removeSql;
                            } catch (SQLException e) {
                                throw new RuntimeException(new JdbcException("open delete sql fail: " + e.getMessage(), e));
                            }
                        })
                        .collect(Collectors.toList());

                for (PreparedStatement statement : deleteSql) {
                    try (PreparedStatement s = statement) {
                        s.execute();
                    } catch (Throwable e) {
                        for (PreparedStatement s : deleteSql) {
                            try {
                                if (!s.isClosed()) {
                                    s.close();
                                }
                            } catch (SQLException ignore) {
                            }
                        }
                        throw e;
                    }
                }
            }
        });
    }

    private Map<WhiteListRuleType, List<RuleTypeEntity>> getRuleTypes(Connection connection, Set<@NotNull String> ruleIds) throws SQLException {
        try (
                PreparedStatement stm = connection.prepareStatement("SELECT * FROM " + RuleTypeEntity.tableName + " WHERE id in ("
                        + ruleIds.stream().map(id -> "?").collect(Collectors.joining(","))
                        + ")")
        ) {
            Iterator<@NotNull String> itr = ruleIds.iterator();
            for (int i = 0; itr.hasNext(); i++) {
                stm.setString(i, itr.next());
            }

            try (Stream<RuleTypeEntity> rules = map(stm.executeQuery(), RuleTypeEntity.type)) {
                return rules.collect(Collectors.groupingBy(RuleTypeEntity::getRuleType));
            }
        }
    }

    public Map<String, WhiteListRule> getRules(Set<String> ruleIds) throws JdbcException {
        if (ruleIds.isEmpty()) return Collections.emptyMap();
        @SuppressWarnings("unchecked")
        Map<String, WhiteListRule>[] resultHolder = new Map[1];
        executeInTransaction("select rules", () -> {
            try (Connection con = getConnection()) {
                Map<WhiteListRuleType, List<RuleTypeEntity>> types = getRuleTypes(con, ruleIds);
                List<Map.Entry<WhiteListRuleType, PreparedStatement>> selectStm = types.entrySet()
                        .stream()
                        .map(group -> {
                            try {
                                List<RuleTypeEntity> value = group.getValue();
                                PreparedStatement stm = con.prepareStatement(MessageFormat.format("SELECT * from {tableName} where id in ({ids})", new HashMap<String, Object>() {{
                                    put("tableName", inferRuleTableName(group.getKey()));
                                    put("ids", value.stream().map(r -> "?").collect(Collectors.joining(",")));
                                }}));
                                int size = value.size();
                                for (int i = 0; i < size; i++) {
                                    stm.setString(i, value.get(i).getId());
                                }
                                return new AbstractMap.SimpleEntry<>(group.getKey(), stm);
                            } catch (SQLException e) {
                                throw new RuntimeException(new JdbcException("open select rules statement fail: " + e.getMessage(), e));
                            }
                        }).collect(Collectors.toList());
                for (Map.Entry<WhiteListRuleType, PreparedStatement> stm : selectStm) {
                    logSql(stm.getValue());
                    try (Statement ignore = stm.getValue(); Stream<WhiteListRule> rules = map(stm.getValue().executeQuery(), stm.getKey())) {
                        resultHolder[0] = rules.collect(Collectors.toMap(WhiteListRule::getId, Function.identity()));
                    } catch (Throwable e) {
                        for (Map.Entry<WhiteListRuleType, PreparedStatement> s : selectStm) {
                            try {
                                if (!s.getValue().isClosed()) {
                                    s.getValue().close();
                                }
                            } catch (SQLException ignore) {
                            }
                        }
                        throw e;
                    }
                }
            }
        });
        return resultHolder[0];
    }

    private static void logSql(PreparedStatement sql) {
        log.debug("execute sql: \n{}", sql);
    }

    protected void sneakyClose(ResultSet resultSet) {
        try {
            if (resultSet.isClosed()) return;
            resultSet.close();
        } catch (SQLException ignore) {
        }
    }

    public <E> @Resource Stream<E> map(ResultSet resultSet, EntityProvider<E> entityProvider) {
        Iterator<E> it = new Iterator<E>() {
            @Override
            public boolean hasNext() {
                try {
                    return resultSet.next();
                } catch (SQLException e) {
                    sneakyClose(resultSet);
                    throw new RuntimeException(new JdbcException("read result set fail: " + e.getMessage(), e));
                }
            }

            @Override
            public E next() {
                Map<BeanProperty, EntityFieldMapper> mappers = getMappers(entityProvider);
                Map<String, PojoField> state = new HashMap<>();
                for (BeanProperty property : entityProvider.getSchema()) {
                    Object jdbcValue;
                    try {
                        jdbcValue = resultSet.getObject(property.getName());
                    } catch (SQLException e) {
                        sneakyClose(resultSet);
                        throw new RuntimeException(new JdbcException("get property from ResultSet fail: " + e.getMessage(), e));
                    }
                    Serializable value = mappers.get(property).mapToPojoFieldValue(jdbcProperties, new EntityFieldMapper.JdbcMapContext(
                            new PojoField(property, null),
                            property.getName(),
                            jdbcValue
                    ));
                    state.put(property.getName(), new PojoField(property, value));
                }
                return entityProvider.populateStates(entityProvider.createPlain(), state);
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.DISTINCT), false)
                .onClose(() -> {
                    try {
                        if (!resultSet.isClosed()) {
                            resultSet.close();
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(new JdbcException("close result set fail: " + e.getMessage(), e));
                    }
                });
    }

    protected void startTransaction(String transactionName) throws JdbcException {
        try {
            Connection connection = getConnection();
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new JdbcException("start '" + transactionName + "' transaction fail: " + e.getMessage(), e);
        }
    }

    protected void executeInTransaction(String transactionName, TransactionTask task) throws JdbcException {
        executeInTransaction(transactionName, Throwable.class, task);
    }

    protected void executeInTransaction(String transactionName, Class<? extends Throwable> rollbackEx, TransactionTask task) throws JdbcException {
        startTransaction(transactionName);
        try {
            task.execute();
        } catch (Throwable e) {
            if (rollbackEx.isInstance(e)) {
                rollback(transactionName);
            } else {
                commit(transactionName);
            }
            if (e instanceof JdbcException) {
                throw (JdbcException) e;
            }
            if (e.getMessage() == null && e.getCause() instanceof JdbcException) {
                throw (JdbcException) e.getCause();
            }
            throw new JdbcException("execution fail: " + e.getMessage(), e);
        }
        commit(transactionName);
    }

    protected void rollback(String transactionName) throws JdbcException {
        try {
            tryGetConnection().ifPresent(con -> {
                connection.remove();
                try {
                    con.rollback();
                    con.close();
                } catch (SQLException e) {
                    throw new RuntimeException(new JdbcException("rollback transaction '" + transactionName + "' fail: " + e.getMessage(), e));
                }
            });
        } catch (Exception e) {
            if (e.getCause() instanceof JdbcException) {
                throw ((JdbcException) e.getCause());
            }
            throw e;
        }
    }

    protected void commit(String transactionName) throws JdbcException {
        try {
            tryGetConnection().ifPresent(con -> {
                connection.remove();
                try {
                    con.commit();
                    con.close();
                } catch (SQLException e) {
                    throw new RuntimeException(new JdbcException("commit transaction '" + transactionName + "' fail: " + e.getMessage(), e));
                }
            });
        } catch (Exception e) {
            if (e.getCause() instanceof JdbcException) {
                throw ((JdbcException) e.getCause());
            }
            throw e;
        }
    }

    @NotNull
    protected Map<BeanProperty, EntityFieldMapper> getMappers(EntityProvider<?> provider) {
        return fieldMappers.computeIfAbsent(provider, c ->
                provider.getSchema().stream()
                        .map(p -> {
                            PojoField field = new PojoField(p, null);
                            EntityFieldMapper mapper = RuleEntityFieldMapperProvider.getInstance().getMappers().stream()
                                    .filter(m -> m.support(provider, jdbcProperties, field))
                                    .findFirst()
                                    .orElseThrow(() -> new UnsupportedFieldTypeException("Unsupported Field-Value '" + field + "' of Rule '" + provider + "'. Value Type is '" + field.getProperty().getPropertyType() + "'"));
                            return new AbstractMap.SimpleEntry<>(p, mapper);
                        })
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }


    protected void createTableIfNeeded(WhiteListRule rule) throws JdbcException {
        Class<? extends WhiteListRule> entityClass = rule.getClass();
        if (entityTableGenerated.contains(entityClass)) {
            return;
        }
        synchronized (this) {
            if (entityTableGenerated.contains(entityClass)) {
                return;
            }

            String createTemplate = generateTableCreateSQL(inferRuleTableName(rule.getType()), generateTableSchema(rule.getType()));
            try (Connection con = getConnection(); Statement stm = con.createStatement()) {
                stm.execute(createTemplate);
            } catch (SQLException e) {
                throw new JdbcException("create table of '" + rule + "' fail: " + e.getMessage(), e);
            }
            entityTableGenerated.add(entityClass);
        }
    }

    protected void prepareBuiltinTable() throws JdbcException {
        String ruleTypeTable = generateTableCreateSQL(RuleTypeEntity.tableName, generateTableSchema(RuleTypeEntity.type));
        String ruleSetEntityTable = generateTableCreateSQL(RuleSetEntity.tableName, generateTableSchema(RuleSetEntity.type));
        String ruleSetElementTable = generateTableCreateSQL(RuleSetElementEntity.tableName, generateTableSchema(RuleSetElementEntity.type));
        try (Connection con = getConnection(); Statement ruleTypeTableStm = con.createStatement(); Statement ruleSetEntityTableStm = con.createStatement(); Statement ruleSetElementTableStm = con.createStatement()) {
            ruleTypeTableStm.execute(ruleTypeTable);
            ruleSetEntityTableStm.execute(ruleSetEntityTable);
            ruleSetElementTableStm.execute(ruleSetElementTable);
        } catch (SQLException e) {
            throw new JdbcException("create builtin table '" + RuleTypeEntity.tableName + "' fail: " + e.getMessage(), e);
        }
    }

    public String inferRuleTableName(WhiteListRuleType type) {
        return RULE_TABLE_PREFIX + type;
    }

    protected String generateTableCreateSQL(String tableName, String schemaSeg) {
        return MessageFormat.format(
                "CREATE TABLE IF NOT EXIST {tableName} ({tableSchema});",
                new HashMap<String, Object>() {{
                    put("tableName", tableName);
                    put("tableSchema", schemaSeg);
                }}
        );
    }

    protected String generateTableSchema(EntityProvider<?> provider) {
        List<EntityFieldMapper> mappers = RuleEntityFieldMapperProvider.getInstance().getMappers();

        List<String> schemaSeg = new ArrayList<>(provider.getSchema().size());
        for (BeanProperty property : provider.getSchema()) {
            PojoField field = new PojoField(property, null);
            EntityFieldMapper mapper = mappers.stream().filter(m -> m.support(provider, jdbcProperties, field)).findFirst()
                    .orElseThrow(() -> new UnsupportedFieldTypeException("Unsupported Property '" + property + "'."));
            String jdbcFieldType = mapper.getJdbcFieldType(provider, jdbcProperties, field);
            schemaSeg.add(property.getName() + " " + jdbcFieldType);
        }

        if (provider.getSchema().stream().anyMatch(p -> "id".equals(p.getName()))) {
            schemaSeg.add("PRIMARY KEY (id)");
        }

        return String.join(",", schemaSeg);
    }

    @Override
    public void close() throws Exception {
        if (dataSource instanceof AutoCloseable) {
            ((AutoCloseable) dataSource).close();
        }
        entityTableGenerated.clear();
        fieldMappers.clear();
    }

    @RequiredArgsConstructor
    private class ConnectionProxy implements Connection {

        @Delegate
        private final Connection target;

        public void close() throws SQLException {
            try {
                target.close();
            } finally {
                connection.remove();
            }
        }

        public void commit() throws SQLException {
            try {
                target.commit();
            } finally {
                target.setAutoCommit(true);
            }
        }

        public void rollback() throws SQLException {
            try {
                target.rollback();
            } finally {
                target.setAutoCommit(true);
            }
        }

    }

    public interface TransactionTask {

        void execute() throws Throwable;
    }

    @Data
    public static class RuleTypeEntity {
        private String id;
        private WhiteListRuleType ruleType;
        public static final String tableName = DCHECK_TABLE_PREFIX + "wlr_type";
        public static final EntityProvider<RuleTypeEntity> type = EntityProvider.getDefaultProvider(RuleTypeEntity.class, RuleTypeEntity::new);
    }

    @Data
    public static class RuleSetEntity {
        private String id;
        private String description;
        public static final String tableName = DCHECK_TABLE_PREFIX + "wlr_ruleSet";
        public static final EntityProvider<RuleSetEntity> type = EntityProvider.getDefaultProvider(RuleSetEntity.class, RuleSetEntity::new);
    }

    @Data
    public static class RuleSetElementEntity {
        private String id;
        @Index
        private String ruleSetId;
        @Index
        private String ruleId;
        private Boolean enabled;
        private Integer order;
        public static final String tableName = DCHECK_TABLE_PREFIX + "wlr_ruleSetElement";
        public static final EntityProvider<RuleSetElementEntity> type = EntityProvider.getDefaultProvider(RuleSetElementEntity.class, RuleSetElementEntity::new);
    }
}
