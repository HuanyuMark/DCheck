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
public class DoubleMapper implements EntityFieldMapper {

    @Override
    public boolean support(EntityProvider<?> entity, Properties jdbcProperties, PojoField kv) {
        return support(kv);
    }

    protected boolean support(PojoField kv) {
        return kv.getProperty().getPropertyType().isAssignableFrom(Double.class);
    }

    @Override
    public String getJdbcFieldType(EntityProvider<?> entity, Properties jdbcProperties, PojoField kv) {
        return "DOUBLE";
    }

    @Override
    public Serializable mapToPojoFieldValue(Properties jdbcProperties, JdbcMapContext mapContext) {
        if (!support(mapContext.getPojoField())) {
            throw new IllegalArgumentException("not support");
        }
        if (mapContext.getJdbcFieldValue() instanceof Double) {
            return (Serializable) mapContext.getJdbcFieldValue();
        }
        throw new IllegalArgumentException("not support");
    }
}
