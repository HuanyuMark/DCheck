package org.example.dcheck.impl.wlm.jdbc.mapper;

import org.example.dcheck.api.EntityProvider;
import org.example.dcheck.api.PojoField;
import org.example.dcheck.api.WhiteListRuleType;
import org.example.dcheck.impl.wlm.jdbc.api.EntityFieldMapper;

import java.io.Serializable;
import java.util.Properties;

/**
 * Date 2025/03/31
 *
 * @author 三石而立Sunsy
 */
public class WhiteListTypeMapper implements EntityFieldMapper {
    @Override
    public boolean support(EntityProvider<?> entity, Properties jdbcProperties, PojoField kv) {
        return support(kv);
    }

    protected boolean support(PojoField kv) {
        return WhiteListRuleType.class.isAssignableFrom(kv.getProperty().getPropertyType());
    }

    @Override
    public String getJdbcFieldType(EntityProvider<?> entity, Properties jdbcProperties, PojoField kv) {
        return "VARCHAR(255)";
    }

    @Override
    public Serializable mapToPojoFieldValue(Properties jdbcProperties, JdbcMapContext mapContext) {
        if (mapContext.getJdbcFieldValue() instanceof String) {
            WhiteListRuleType type = WhiteListRuleType.ALL_TYPES.get((String) mapContext.getJdbcFieldValue());
            if (type == null) {
                throw new IllegalArgumentException("unknown ruleType: " + mapContext.getJdbcFieldValue());
            }
            return type;
        }
        throw new IllegalArgumentException("not support");
    }
}
