package org.example.dcheck.impl.wlm.jdbc.mapper;

import lombok.Getter;
import org.example.dcheck.api.EntityProvider;
import org.example.dcheck.api.PojoField;
import org.example.dcheck.impl.wlm.jdbc.api.EntityFieldMapper;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
@Getter
public class StringMapper implements EntityFieldMapper {
    protected final Set<String> excludeFields = new HashSet<>();

    public StringMapper() {
        excludeFields.add("id");
    }

    @Override
    public boolean support(EntityProvider<?> entity, Properties jdbcProperties, PojoField kv) {
        return support(kv);
    }

    protected boolean support(PojoField kv) {
        return kv.getProperty().getPropertyType().isAssignableFrom(String.class)
                && !excludeFields.contains(kv.getProperty().getName());
    }

    @Override
    public Serializable mapToPojoFieldValue(Properties jdbcProperties, JdbcMapContext mapContext) {
        if (mapContext.getJdbcFieldValue() instanceof String) {
            return (Serializable) mapContext.getJdbcFieldValue();
        }
        throw new IllegalArgumentException("not support");
    }


    @Override
    public String getJdbcFieldType(EntityProvider<?> entity, Properties jdbcProperties, PojoField kv) {
        return "TEXT";
    }
}
