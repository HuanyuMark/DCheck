package org.example.dcheck.impl.alm.jdbc.mapper;

import lombok.Getter;
import org.example.dcheck.api.EntityProvider;
import org.example.dcheck.api.PojoField;
import org.example.dcheck.impl.alm.jdbc.api.EntityFieldMapper;
import org.example.dcheck.impl.alm.jdbc.support.JdbcAgent;

import java.io.Serializable;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
@Getter
public class DoubleMapper implements EntityFieldMapper {

    @Override
    public boolean support(JdbcAgent agent, EntityProvider<?> entity, PojoField kv) {
        return support(kv);
    }

    protected boolean support(PojoField kv) {
        return kv.getProperty().getPropertyType().isAssignableFrom(Double.class);
    }

    @Override
    public String getJdbcFieldType(JdbcAgent agent, EntityProvider<?> entity, PojoField kv) {
        return "DOUBLE";
    }

    @Override
    public Serializable mapToPojoFieldValue(JdbcAgent agent, JdbcMapContext mapContext) {
        if (!support(mapContext.getPojoField())) {
            throw new IllegalArgumentException("not support");
        }
        if (mapContext.getJdbcFieldValue() instanceof Double) {
            return (Serializable) mapContext.getJdbcFieldValue();
        }
        throw new IllegalArgumentException("not support");
    }
}
