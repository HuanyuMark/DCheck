package org.example.dcheck.impl.wlm.jdbc.mapper;

import org.example.dcheck.api.EntityProvider;
import org.example.dcheck.api.PojoField;
import org.example.dcheck.impl.wlm.jdbc.api.EntityFieldMapper;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

/**
 * Date 2025/03/31
 *
 * @author 三石而立Sunsy
 */
public class BooleanMapper implements EntityFieldMapper {

    @Override
    public boolean support(EntityProvider<?> entity, Properties jdbcProperties, PojoField kv) {
        return kv.getProperty().getPropertyType().isAssignableFrom(Boolean.class);
    }

    @Override
    public String getJdbcFieldType(EntityProvider<?> entity, Properties jdbcProperties, PojoField kv) {
        return "VARCHAR(1)";
    }

    @Override
    public Serializable mapToPojoFieldValue(Properties jdbcProperties, JdbcMapContext mapContext) {
        return "Y".equals(mapContext.getJdbcFieldValue()) || "N".equals(mapContext.getJdbcFieldValue());
    }

    @Override
    public Serializable mapToJdbcFieldValue(Properties jdbcProperties, EntityProvider<?> entity, Map.Entry<String, PojoField> pojoState) {
        return (pojoState.getValue().getValue() != null && (Boolean) pojoState.getValue().getValue()) ? "Y" : "N";
    }
}
