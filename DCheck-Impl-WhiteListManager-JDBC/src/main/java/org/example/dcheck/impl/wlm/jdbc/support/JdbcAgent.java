package org.example.dcheck.impl.wlm.jdbc.support;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.api.DuplicatePart;
import org.example.dcheck.api.PojoField;
import org.example.dcheck.api.WhiteListRule;
import org.example.dcheck.api.WhiteListRuleType;
import org.example.dcheck.common.util.MessageFormat;
import org.example.dcheck.impl.wlm.jdbc.api.BatchInsertOrUpdateSQLGenerator;
import org.example.dcheck.impl.wlm.jdbc.api.RuleEntityFieldMapper;
import org.example.dcheck.impl.wlm.jdbc.exception.JdbcException;
import org.example.dcheck.impl.wlm.jdbc.exception.UnsupportedFieldTypeException;
import org.example.dcheck.spi.BatchInsertOrUpdateSQLGeneratorProvider;
import org.example.dcheck.spi.DCheckConfigProvider;
import org.example.dcheck.spi.RuleEntityFieldMapperProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.NonNull;

import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    protected final Map<Class<? extends WhiteListRule>, List<RuleEntityFieldMapper>> fieldMappers = new ConcurrentHashMap<>();

    protected final BatchInsertOrUpdateSQLGenerator batchInsertOrUpdateSQLGenerator;

    protected static final String DCHECK_TABLE_PREFIX = "dcheck_";

    protected static final String RULE_TABLE_PREFIX = DCHECK_TABLE_PREFIX + "wlr_state_";
    @Getter
    protected final String ruleTypeTableName = DCHECK_TABLE_PREFIX + "wlr_type";

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
            List<RuleEntityFieldMapper> mappers = getMappers(rule, state);

            Iterator<RuleEntityFieldMapper> mapperItr = mappers.iterator();

            Map<String, Serializable> mapped = new LinkedHashMap<>(state.size(), 1);
            for (Map.Entry<String, PojoField> entry : state.entrySet()) {
                mapped.put(entry.getKey(), mapperItr.next().mapToJdbcFieldValue(jdbcProperties, rule, entry));
            }

            return new DefaultMappedRuleEntity(rule, mapped);
        }).collect(Collectors.groupingBy(e -> e.getRule().getType()));

        executeInTransaction("insert/update", () -> {
            Map<WhiteListRuleType, PreparedStatement> sqls = batchInsertOrUpdateSQLGenerator.generateMergeRuleEntity(this, mappedGroups);
            for (PreparedStatement sql : sqls.values()) {
                try (PreparedStatement ignored = sql) {
                    log.debug("execute sql: \n{}", sql.toString());
                    sql.execute();
                }
            }
            try (PreparedStatement storeTypeSql = batchInsertOrUpdateSQLGenerator.generateMergeEntityType(this, rules)) {
                log.debug("execute sql: \n{}", storeTypeSql.toString());
                storeTypeSql.execute();
            }
        });
    }

    public void remove(List<@NotNull String> ruleIds) throws JdbcException {
        if (ruleIds.isEmpty()) return;
        executeInTransaction("remove", () -> {
            try (Connection con = getConnection();
                 PreparedStatement stm = con.prepareStatement(MessageFormat.format("SELECT * FROM {tableName} WHERE id in ({ids})", new HashMap<String, Object>() {{
                     put("tableName", getRuleTypeTableName());
                     put("ids", ruleIds.stream().map(id -> "?").collect(Collectors.joining(",")));
                 }}))) {
                int size = ruleIds.size();
                for (int i = 0; i < size; i++) {
                    stm.setString(i, ruleIds.get(i));
                }
//                con.getMetaData().getTa
                try (ResultSet resultSet = stm.executeQuery()) {
                    //TODO auto map to entity
                    String id = resultSet.getString("id");
                    String ruleTypeStr = resultSet.getString("ruleType");
                    WhiteListRuleType ruleType = WhiteListRuleType.ALL_TYPES.get(ruleTypeStr);
                    if (ruleType == null) {
                        throw new JdbcException("unknown ruleType: " + ruleTypeStr);
                    }
                }
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
    private List<RuleEntityFieldMapper> getMappers(WhiteListRule rule, Map<String, PojoField> state) {
        return fieldMappers.computeIfAbsent(rule.getClass(), c ->
                state.entrySet().stream()
                        .map(kv ->
                                RuleEntityFieldMapperProvider.getInstance().getMappers().stream()
                                        .filter(m -> m.support(rule, jdbcProperties, kv.getValue()))
                                        .findFirst()
                                        .orElseThrow(() -> new UnsupportedFieldTypeException("Unsupported Field-Value '" + kv + "' of Rule '" + rule + "'. Value Type is '" + (kv.getValue() != null ? kv.getValue().getClass() : null) + "'"))
                        )
                        .collect(Collectors.toList())
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

            String createTemplate = generateTableCreateSQL(inferRuleTableName(rule), generateTableSchema(rule));
            try (Connection con = getConnection(); Statement stm = con.createStatement()) {
                stm.execute(createTemplate);
            } catch (SQLException e) {
                throw new JdbcException("create table of '" + rule + "' fail: " + e.getMessage(), e);
            }
            entityTableGenerated.add(entityClass);
        }
    }

    protected void prepareBuiltinTable() throws JdbcException {
        String createTemplate = generateTableCreateSQL(getRuleTypeTableName(), generateTableSchema(new RuleType("", "")));
        try (Connection con = getConnection(); Statement stm = con.createStatement()) {
            stm.execute(createTemplate);
        } catch (SQLException e) {
            throw new JdbcException("create builtin table '" + getRuleTypeTableName() + "' fail: " + e.getMessage(), e);
        }
    }

    public String inferRuleTableName(WhiteListRule rule) {
        return RULE_TABLE_PREFIX + rule.getType();
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

    protected String generateTableSchema(WhiteListRule rule) {
        List<RuleEntityFieldMapper> mappers = RuleEntityFieldMapperProvider.getInstance().getMappers();

        Map<String, PojoField> state = rule.getState();

        List<String> schemaSeg = new ArrayList<>(state.size());

        for (Map.Entry<String, PojoField> entry : state.entrySet()) {
            RuleEntityFieldMapper mapper = mappers.stream().filter(m -> m.support(rule, jdbcProperties, entry.getValue())).findFirst()
                    .orElseThrow(() -> new UnsupportedFieldTypeException("Unsupported Field-Value '" + entry + "' of Rule '" + rule + "'. Value Type is '" + (entry.getValue() != null ? entry.getValue().getClass() : null) + "'"));
            String jdbcFieldType = mapper.getJdbcFieldType(rule, jdbcProperties, entry.getValue());
            schemaSeg.add(entry.getKey() + " " + jdbcFieldType);
        }

        if (state.containsKey("id")) {
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
    protected static class RuleType implements WhiteListRule {
        private final String id;
        private final String ruleType;

        @Override
        public @NotNull String getId() {
            return "TypeRule";
        }

        public String getRuleType() {
            return "RuleType";
        }

        @Override
        public @NotNull WhiteListRuleType getType() {
            return new WhiteListRuleType() {
                @Override
                public @NotNull String name() {
                    return "TypeRule";
                }

                @Override
                public @NotNull Class<?> getType() {
                    return RuleType.class;
                }
            };
        }

        @Override
        public @NotNull List<@NotNull DuplicatePart> calculateFilterScore(@NotNull List<@NotNull DuplicatePart> paragraphs, FilterContext handler) {
            return paragraphs;
        }
    }
}
