package org.example.dcheck.impl.alm.jdbc.mapper;

import lombok.Getter;
import org.example.dcheck.api.EntityProvider;
import org.example.dcheck.impl.alm.jdbc.api.EntityFieldMapper;
import org.example.dcheck.impl.alm.jdbc.support.JdbcAgent;
import org.example.dcheck.util.PropertyValue;

import java.io.Serializable;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
@Getter
public class IntegerMapper implements EntityFieldMapper {

    @Override
    public boolean support(JdbcAgent agent, EntityProvider<?> entity, PropertyValue kv) {
        return support(kv);
    }

    protected boolean support(PropertyValue kv) {
        return kv.getProperty().getPropertyType().isAssignableFrom(Integer.class);
    }

    @Override
    public String getJdbcFieldType(JdbcAgent agent, EntityProvider<?> entity, PropertyValue kv) {
        return "INT";
    }

    @Override
    public Serializable mapToPojoFieldValue(JdbcAgent agent, JdbcMapContext mapContext) {
        if (mapContext.getJdbcFieldValue() instanceof Integer) {
            return (Serializable) mapContext.getJdbcFieldValue();
        }
        if (mapContext.getJdbcFieldValue() instanceof String) {
            try {
                return Integer.valueOf((String) mapContext.getJdbcFieldValue());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("map jdbc value to Integer fail: " + e.getMessage(), e);
            }
        }
        throw new IllegalArgumentException("not support");
    }
}
