package org.example.dcheck.impl.wlm.jdbc.api;

import lombok.Data;
import org.example.dcheck.api.PojoField;
import org.example.dcheck.api.WhiteListRule;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
public interface RuleEntityFieldMapper {
    boolean support(WhiteListRule rule, Properties jdbcProperties, PojoField kv);

    String getJdbcFieldType(WhiteListRule rule, Properties jdbcProperties, PojoField kv);

    Serializable mapToPojoFieldValue(Properties jdbcProperties, JdbcMapContext mapContext);

    default Serializable mapToJdbcFieldValue(Properties jdbcProperties, WhiteListRule rule, Map.Entry<String, PojoField> pojoState) {
        return pojoState.getValue().getValue();
    }

    @Data
    class JdbcMapContext {
        private final PojoField pojoField;
        private final String jdbcFieldName;
        private final Serializable jdbcFieldValue;
    }
}
