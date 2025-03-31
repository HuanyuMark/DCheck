package org.example.dcheck.impl.wlm.jdbc.api;

import lombok.Data;
import org.example.dcheck.api.EntityProvider;
import org.example.dcheck.api.PojoField;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
public interface EntityFieldMapper {
    boolean support(EntityProvider<?> entity, Properties jdbcProperties, PojoField kv);

    String getJdbcFieldType(EntityProvider<?> entity, Properties jdbcProperties, PojoField kv);

    /**
     * we assume that this mapper is {@link #support} the field value.
     * you should call {@link #support}(if return {@code true}) before call this method
     */
    Serializable mapToPojoFieldValue(Properties jdbcProperties, JdbcMapContext mapContext);

    default Serializable mapToJdbcFieldValue(Properties jdbcProperties, EntityProvider<?> entity, Map.Entry<String, PojoField> pojoState) {
        return pojoState.getValue().getValue();
    }

    @Data
    class JdbcMapContext {
        private final PojoField pojoField;
        private final String jdbcFieldName;
        private final Object jdbcFieldValue;
    }
}
