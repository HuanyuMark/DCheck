package org.example.dcheck.impl.alm.jdbc.mapper;

import org.example.dcheck.api.EntityProvider;
import org.example.dcheck.impl.alm.jdbc.api.EntityFieldMapper;
import org.example.dcheck.impl.alm.jdbc.support.JdbcAgent;
import org.example.dcheck.util.PropertyValue;

import java.io.Serializable;
import java.util.Map;

/**
 * Date 2025/03/31
 *
 * @author 三石而立Sunsy
 */
public class BooleanMapper implements EntityFieldMapper {

    @Override
    public boolean support(JdbcAgent agent, EntityProvider<?> entity, PropertyValue kv) {
        return kv.getProperty().getPropertyType().isAssignableFrom(Boolean.class);
    }

    @Override
    public String getJdbcFieldType(JdbcAgent agent, EntityProvider<?> entity, PropertyValue kv) {
        return "VARCHAR(1)";
    }

    @Override
    public Serializable mapToPojoFieldValue(JdbcAgent agent, JdbcMapContext mapContext) {
        return "Y".equals(mapContext.getJdbcFieldValue()) || "N".equals(mapContext.getJdbcFieldValue());
    }

    @Override
    public Object mapToJdbcFieldValue(JdbcAgent agent, EntityProvider<?> entity, Map.Entry<String, PropertyValue> pojoState) {
        Object value = pojoState.getValue().getValue();
        return (value != null && (Boolean) value) ? "Y" : "N";
    }
}
