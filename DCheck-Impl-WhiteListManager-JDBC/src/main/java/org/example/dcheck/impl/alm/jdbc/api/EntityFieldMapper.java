package org.example.dcheck.impl.alm.jdbc.api;

import lombok.Data;
import org.example.dcheck.api.EntityProvider;
import org.example.dcheck.api.PojoField;
import org.example.dcheck.impl.alm.jdbc.support.JdbcAgent;

import java.io.Serializable;
import java.util.Map;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
public interface EntityFieldMapper {
    boolean support(JdbcAgent agent, EntityProvider<?> entity, PojoField kv);

    String getJdbcFieldType(JdbcAgent agent, EntityProvider<?> entity, PojoField kv);

    /**
     * we assume that this mapper is {@link #support} the field value.
     * you should call {@link #support}(if return {@code true}) before call this method
     */
    Serializable mapToPojoFieldValue(JdbcAgent agent, JdbcMapContext mapContext);

    default Object mapToJdbcFieldValue(JdbcAgent agent, EntityProvider<?> entity, Map.Entry<String, PojoField> pojoState) {
        return pojoState.getValue().getValue();
    }

    @Data
    class JdbcMapContext {
        private final PojoField pojoField;
        private final String jdbcFieldName;
        private final Object jdbcFieldValue;
    }
}
