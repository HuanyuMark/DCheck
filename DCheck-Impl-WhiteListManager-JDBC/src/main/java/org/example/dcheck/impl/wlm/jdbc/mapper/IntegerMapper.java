package org.example.dcheck.impl.wlm.jdbc.mapper;

import lombok.Getter;
import org.example.dcheck.api.EntityProvider;
import org.example.dcheck.api.PojoField;
import org.example.dcheck.impl.wlm.jdbc.api.EntityFieldMapper;

import java.io.Serializable;
import java.util.Properties;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
@Getter
public class IntegerMapper implements EntityFieldMapper {

    @Override
    public boolean support(EntityProvider<?> entity, Properties jdbcProperties, PojoField kv) {
        return support(kv);
    }

    protected boolean support(PojoField kv) {
        return kv.getProperty().getPropertyType().isAssignableFrom(Integer.class);
    }

    @Override
    public String getJdbcFieldType(EntityProvider<?> entity, Properties jdbcProperties, PojoField kv) {
        return "INT";
    }

    @Override
    public Serializable mapToPojoFieldValue(Properties jdbcProperties, JdbcMapContext mapContext) {
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
