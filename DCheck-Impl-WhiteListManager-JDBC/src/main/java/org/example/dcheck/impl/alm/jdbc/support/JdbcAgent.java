package org.example.dcheck.impl.alm.jdbc.support;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.annotation.Ignore;
import org.example.dcheck.api.AllowListRule;
import org.example.dcheck.api.AllowListRuleType;
import org.example.dcheck.api.EntityProvider;
import org.example.dcheck.common.util.MessageFormat;
import org.example.dcheck.impl.alm.jdbc.annotation.Index;
import org.example.dcheck.impl.alm.jdbc.api.EntityFieldMapper;
import org.example.dcheck.impl.alm.jdbc.api.JdbcDelegator;
import org.example.dcheck.impl.alm.jdbc.entity.RuleSetElementEntity;
import org.example.dcheck.impl.alm.jdbc.entity.RuleSetEntity;
import org.example.dcheck.impl.alm.jdbc.entity.RuleTypeEntity;
import org.example.dcheck.impl.alm.jdbc.exception.JdbcException;
import org.example.dcheck.impl.alm.jdbc.exception.UnsupportedFieldTypeException;
import org.example.dcheck.spi.JdbcDelegatorProvider;
import org.example.dcheck.spi.RuleEntityFieldMapperProvider;
import org.example.dcheck.util.BeanProperty;
import org.example.dcheck.util.PropertyValue;
import org.jetbrains.annotations.NotNull;

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

    @Getter
    protected final DataSourceInfo dataSourceInfo;

    protected final ThreadLocal<Connection> connection = new ThreadLocal<>();

    protected final Set<AllowListRuleType> entityTableGenerated = ConcurrentHashMap.newKeySet();

    protected final Map<EntityProvider<?>, Map<BeanProperty, EntityFieldMapper>> fieldMappers = new ConcurrentHashMap<>();

    protected final JdbcDelegator jdbcDelegator;

    public static final String DCHECK_TABLE_PREFIX = "dcheck_";

    public static final String RULE_TABLE_PREFIX = DCHECK_TABLE_PREFIX + "alr_ruleState_";

    public JdbcAgent(@NonNull DataSourceInfo dataSourceInfo, @NonNull DataSource dataSource) throws JdbcException {
        this.dataSourceInfo = dataSourceInfo;
        this.dataSource = dataSource;
        jdbcDelegator = JdbcDelegatorProvider.getInstance().find(this);
        prepareBuiltinTable();
    }

    public Map<String, AllowListRule> selectRule(List<String> ruleIds) throws JdbcException {
        if (ruleIds.isEmpty()) return Collections.emptyMap();
        Map<String, AllowListRule> resultHolder = new LinkedHashMap<>((int) (ruleIds.size() / 0.7));
        try (Connection con = getConnection()) {
            Map<AllowListRuleType, List<RuleTypeEntity>> types = getRuleTypes(con, ruleIds);
            List<Map.Entry<AllowListRuleType, PreparedStatement>> selectStm = types.entrySet()
                    .stream()
                    .map(group -> {
                        try {
                            List<RuleTypeEntity> value = group.getValue();
                            PreparedStatement stm = con.prepareStatement(MessageFormat.format("SELECT * FROM {tableName} WHERE `id` IN ({ids})", new HashMap<String, Object>() {{
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
            for (Map.Entry<AllowListRuleType, PreparedStatement> stm : selectStm) {
                logSql(stm.getValue());
                try (Statement ignore = stm.getValue(); Stream<AllowListRule> rules = map(stm.getValue().executeQuery(), stm.getKey())) {
                    resultHolder.putAll(rules.onClose(() -> {
                        for (Map.Entry<AllowListRuleType, PreparedStatement> s : selectStm) {
                            try {
                                if (!s.getValue().isClosed()) {
                                    s.getValue().close();
                                }
                            } catch (SQLException ignore2) {
                            }
                        }
                    }).collect(Collectors.toMap(AllowListRule::getId, Function.identity())));
                }
            }
        } catch (SQLException e) {
            throw new JdbcException("Execute selectRule fail: " + e.getMessage(), e);
        }
        return resultHolder;
    }

    public Map<String, RuleSetElementEntity> selectRuleSetElementByRuleSetId(@NonNull String ruleSetId) throws JdbcException {
        return selectRuleSetElementAndWhere(ruleSetId, "", stm -> {
        });
    }

    protected final EntityFieldMapper enabledMapper = getMappers(RuleSetElementEntity.provider).entrySet().stream().filter(e -> e.getKey().equals(RuleSetElementEntity.getEnabledProperty())).findFirst().orElseThrow(IllegalStateException::new).getValue();


    public Map<String, RuleSetElementEntity> selectRuleSetElementByRuleSetIdAndEnabled(String ruleSetId, boolean enabled) throws JdbcException {
        return selectRuleSetElementAndWhere(ruleSetId, "AND `enabled`=?", stm ->
                stm.setObject(1, enabledMapper.mapToJdbcFieldValue(this, RuleSetElementEntity.provider,
                        new AbstractMap.SimpleEntry<>("enabled", new PropertyValue(RuleSetElementEntity.getEnabledProperty(), enabled))))
        );
    }

    public Optional<RuleSetElementEntity> selectRuleSetElementByRuleSetIdAndRuleId(@NonNull String ruleSetId, @NonNull String ruleId) throws JdbcException {
        return selectRuleSetElementAndWhere(ruleSetId, "AND `id`=?", stm -> {
            stm.setString(1, ruleId);
        }).entrySet().stream().findFirst().map(Map.Entry::getValue);
    }


    public void mergeRule(List<AllowListRule> rules) throws JdbcException {
        if (rules.isEmpty()) return;

        for (AllowListRule rule : rules) {
            createTableIfNeeded(rule.getType());
        }

        Map<AllowListRuleType, List<JdbcDelegator.MappedRuleEntity>> mappedGroups = rules.stream().map(rule -> new DefaultMappedRuleEntity(rule, mapToJdbcValues(rule.getType(), rule))).collect(Collectors.groupingBy(e -> e.getRule().getType()));

        executeInTransaction("mergeRule", () -> {
            Map<AllowListRuleType, PreparedStatement> stm = jdbcDelegator.generateMergeRuleEntity(this, mappedGroups);
            for (PreparedStatement sql : stm.values()) {
                try (PreparedStatement ignored = sql) {
                    logSql(sql);
                    sql.execute();
                }
            }
            try (PreparedStatement storeTypeSql = jdbcDelegator.generateMergeEntityType(this, rules)) {
                logSql(storeTypeSql);
                storeTypeSql.execute();
            }
        });
    }

    public void deleteRule(List<@NotNull String> ruleIds) throws JdbcException {
        if (ruleIds.isEmpty()) return;
        executeInTransaction("deleteRule", () -> {
            try (Connection con = getConnection()) {
                Map<AllowListRuleType, List<RuleTypeEntity>> types = getRuleTypes(con, ruleIds);
                List<PreparedStatement> deleteSql = types.entrySet()
                        .stream()
                        .map(group -> {
                            try {
                                PreparedStatement removeSql = con.prepareStatement(MessageFormat.format("DELETE FROM {tableName} WHERE id IN ({ids})", new HashMap<String, Object>() {{
                                    put("tableName", inferRuleTableName(group.getKey()));
                                    put("ids", group.getValue().stream().map(r -> "?").collect(Collectors.joining(",")));
                                }}));
                                removeSql.closeOnCompletion();
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

    public Map<String, RuleSetEntity> selectRuleSet(List<String> ruleSetIds) throws JdbcException {
        if (ruleSetIds.isEmpty()) return Collections.emptyMap();
        Map<String, RuleSetEntity> resultHolder = new HashMap<>((int) (ruleSetIds.size() / 0.7));
        executeInTransaction("selectRuleSet", () -> {
            try (Connection con = getConnection();
                 PreparedStatement stm = con.prepareStatement(String.format(
                         "SELECT * FROM " + RuleSetEntity.tableName + " WHERE id IN (%s)",
                         ruleSetIds.stream().map(r -> "?").collect(Collectors.joining(","))
                 ))) {
                int size = ruleSetIds.size();
                for (int i = 0; i < size; i++) {
                    stm.setString(i, ruleSetIds.get(i));
                }
                try (Stream<RuleSetEntity> stream = map(stm.executeQuery(), RuleSetEntity.provider)) {
                    resultHolder.putAll(stream.collect(Collectors.toMap(RuleSetEntity::getId, Function.identity())));
                }
            }
        });
        return resultHolder;
    }

    public void mergeRuleSet(List<RuleSetEntity> entities) throws JdbcException {
        if (entities.isEmpty()) return;

        List<JdbcDelegator.MappedRuleSetEntity> mappedValues = entities.stream().map(entity -> new DefaultMappedRuleSetEntity(entity, mapToJdbcValues(RuleSetEntity.provider, entity))).collect(Collectors.toList());

        executeInTransaction("mergeRuleSet", () -> jdbcDelegator.executeMergeRuleSetEntity(this, mappedValues));
    }

    protected <E> @NotNull Map<String, Object> mapToJdbcValues(EntityProvider<E> provider, E entity) {
        Map<BeanProperty, EntityFieldMapper> mappers = getMappers(provider);
        Map<String, PropertyValue> state = provider.getState(entity);
        Map<String, Object> mapped = new LinkedHashMap<>((int) (state.size() / 0.7));
        for (Map.Entry<String, PropertyValue> entry : state.entrySet()) {
            mapped.put(entry.getKey(), mappers.get(entry.getValue().getProperty()).mapToJdbcFieldValue(this, provider, entry));
        }
        return mapped;
    }

    /**
     * @return ruleId -> RuleSetElementEntity
     */
    public Map<String, RuleSetElementEntity> selectRuleSetElement(String ruleSetId, List<String> ruleIds) throws JdbcException {
        if (ruleIds.isEmpty()) return Collections.emptyMap();
        try {
            return selectRuleSetElementAndWhere(ruleSetId, "AND " + ruleIds.stream().map(r -> "?").collect(Collectors.joining(",")),
                    stm -> {
                        int size = ruleIds.size();
                        for (int i = 1; i <= size; i++) {
                            stm.setString(i, ruleIds.get(i));
                        }
                    });
        } catch (Throwable e) {
            if (e.getCause() instanceof JdbcException) {
                throw (JdbcException) e.getCause();
            }
            throw new JdbcException("Execute selectRuleSetElement fail: " + e.getMessage(), e);
        }
    }

    protected interface SqlExceptionConsumer<I> {
        void accept(I input) throws SQLException;
    }

    protected Map<String, RuleSetElementEntity> selectRuleSetElementAndWhere(String ruleSetId, String whereStm, SqlExceptionConsumer<PreparedStatement> stmConfigure) throws JdbcException {
        try (Connection con = getConnection();
             PreparedStatement stm = con.prepareStatement(String.format(
                     "SELECT * FROM " + RuleSetElementEntity.tableName + " WHERE ruleSetId=? %s ORDER BY `order` ASC",
                     whereStm
             ))) {

            stm.setString(1, ruleSetId);

            stmConfigure.accept(stm);

            try (Stream<RuleSetElementEntity> stream = map(stm.executeQuery(), RuleSetElementEntity.provider)) {
                return stream.collect(Collectors.toMap(RuleSetElementEntity::getRuleId, Function.identity(), (a, b) -> {
                    throw new IllegalStateException();
                }, LinkedHashMap::new));
            }
        } catch (SQLException e) {
            throw new JdbcException("Execute selectRuleSetElement fail: " + e.getMessage(), e);
        }
    }

    public void deleteElementInRuleSet(String ruleSetId, List<String> ruleIds) throws JdbcException {
        if (ruleIds.isEmpty()) return;

        deleteElementInRuleSetAndWhere(ruleSetId, String.format("AND ruleId IN (%s)", ruleIds.stream().map(r -> "?").collect(Collectors.joining(","))), stm -> {
            int size = ruleIds.size();
            for (int i = 1; i <= size; i++) {
                stm.setString(i, ruleIds.get(i));
            }
        });
    }

    public void deleteElementInRuleSet(String ruleSetId) throws JdbcException {
        deleteElementInRuleSetAndWhere(ruleSetId, "", stm -> {
        });
    }

    protected void deleteElementInRuleSetAndWhere(String ruleSetId, String whereStm, SqlExceptionConsumer<PreparedStatement> stmConfigure) throws JdbcException {
        executeInTransaction("deleteElementInRuleSet", () -> {
            try (Connection con = getConnection();
                 PreparedStatement stm = con.prepareStatement(String.format(
                                 "DELETE " + RuleSetElementEntity.tableName + " WHERE ruleSetId=? %s",
                                 whereStm
                         )
                 )) {

                stm.setString(0, ruleSetId);

                stmConfigure.accept(stm);

                stm.execute();
            }
        });
    }

    public void mergeRuleSetElement(List<RuleSetElementEntity> entities) throws JdbcException {
        if (entities.isEmpty()) return;

        List<JdbcDelegator.MappedRuleSetElementEntity> mappedValues = entities.stream().map(entity -> new DefaultMappedRuleSetElementEntity(entity, mapToJdbcValues(RuleSetElementEntity.provider, entity))).collect(Collectors.toList());

        executeInTransaction("mergeRuleSetElement", () -> jdbcDelegator.executeMergeRuleSetElement(this, mappedValues));
    }

    public Optional<Connection> tryGetConnection() {
        return Optional.ofNullable(this.connection.get());
    }

    public Connection getConnection() throws JdbcException {
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

    private Map<AllowListRuleType, List<RuleTypeEntity>> getRuleTypes(Connection connection, List<@NotNull String> ruleIds) throws SQLException {
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

    public static void logSql(Statement sql) {
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
                Map<String, PropertyValue> state = new LinkedHashMap<>();
                for (BeanProperty property : entityProvider.getSchema()) {
                    Object jdbcValue;
                    try {
                        jdbcValue = resultSet.getObject(property.getName());
                    } catch (SQLException e) {
                        sneakyClose(resultSet);
                        throw new RuntimeException(new JdbcException("get property from ResultSet fail: " + e.getMessage(), e));
                    }
                    Serializable value = mappers.get(property).mapToPojoFieldValue(JdbcAgent.this, new EntityFieldMapper.JdbcMapContext(
                            new PropertyValue(property, null),
                            property.getName(),
                            jdbcValue
                    ));
                    state.put(property.getName(), new PropertyValue(property, value));
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
            if (connection.getAutoCommit()) {
                connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                connection.setAutoCommit(false);
            }
        } catch (SQLException e) {
            throw new JdbcException("start '" + transactionName + "' transaction fail: " + e.getMessage(), e);
        }
    }

    protected void executeInTransaction(String transactionName, ThrowableTask task) throws JdbcException {
        executeInTransaction(transactionName, Throwable.class, task);
    }

    protected void executeInTransaction(String transactionName, Class<? extends Throwable> rollbackEx, ThrowableTask task) throws JdbcException {
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
                            PropertyValue field = new PropertyValue(p, null);
                            EntityFieldMapper mapper = RuleEntityFieldMapperProvider.getInstance().getMappers().stream()
                                    .filter(m -> m.support(this, provider, field))
                                    .findFirst()
                                    .orElseThrow(() -> new UnsupportedFieldTypeException("Unsupported Field-Value '" + field + "' of Rule '" + provider + "'. Value Type is '" + field.getProperty().getPropertyType() + "'"));
                            return new AbstractMap.SimpleEntry<>(p, mapper);
                        })
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    protected void createTableIfNeeded(AllowListRuleType type) throws JdbcException {
        if (entityTableGenerated.contains(type)) {
            return;
        }
        synchronized (this) {
            if (entityTableGenerated.contains(type)) {
                return;
            }

            createTable(type, inferRuleTableName(type));

            entityTableGenerated.add(type);
        }
    }

    protected void prepareBuiltinTable() throws JdbcException {
        createTable(RuleTypeEntity.type, RuleTypeEntity.tableName);
        createTable(RuleSetEntity.provider, RuleSetEntity.tableName);
        createTable(RuleSetElementEntity.provider, RuleSetElementEntity.tableName);
    }

    public String inferRuleTableName(EntityProvider<?> type) {
        return RULE_TABLE_PREFIX + type;
    }

    protected void createTable(EntityProvider<?> provider, String tableName) throws JdbcException {
        Map<BeanProperty, Index.Value> indexes = provider.getSchema()
                .stream()
                .filter(p -> !p.isGetterAnnPresent(Ignore.class))
                .filter(p -> p.isFieldAnnPresent(Index.class))
                .map(p -> {
                    Index index = p.getFieldAnn(Index.class);
                    assert index != null;
                    return new AbstractMap.SimpleEntry<>(p, Index.Value.of(index));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> {
                    throw new IllegalStateException("duplicate index field '" + k1 + "' and '" + k2 + "'");
                }, LinkedHashMap::new));

        jdbcDelegator.executeCreateTable(this, new DefaultTableCreationContext(provider, tableName, indexes));
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

        @Override
        public void close() throws SQLException {
            try {
                target.close();
            } finally {
                connection.remove();
            }
        }

        @Override
        public void commit() throws SQLException {
            try {
                target.commit();
            } finally {
                target.setAutoCommit(true);
            }
        }

        @Override
        public void rollback() throws SQLException {
            try {
                target.rollback();
            } finally {
                target.setAutoCommit(true);
            }
        }

    }

    public interface ThrowableTask {

        void execute() throws Throwable;
    }

    @Data
    public static class DefaultTableCreationContext implements JdbcDelegator.TableCreationContext {
        final EntityProvider<?> entityProvider;
        final String tableName;
        final Map<BeanProperty, Index.Value> indexes;
    }

    @Data
    protected static class DefaultMappedRuleEntity implements JdbcDelegator.MappedRuleEntity {
        protected final AllowListRule rule;
        protected final Map<String, Object> mappedValues;
    }

    @Data
    protected static class DefaultMappedRuleSetElementEntity implements JdbcDelegator.MappedRuleSetElementEntity {
        protected final RuleSetElementEntity entity;
        protected final Map<String, Object> mappedValues;
    }

    @Data
    protected static class DefaultMappedRuleSetEntity implements JdbcDelegator.MappedRuleSetEntity {
        protected final RuleSetEntity ruleSet;
        protected final Map<String, Object> mappedValues;
    }
}
