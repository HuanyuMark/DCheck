package org.example.dcheck.impl.alm.jdbc.mapper;

import org.example.dcheck.api.AllowListRuleType;
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
public class AllowListTypeMapper implements EntityFieldMapper {
    @Override
    public boolean support(JdbcAgent agent, EntityProvider<?> entity, PropertyValue kv) {
        return support(kv);
    }

    protected boolean support(PropertyValue kv) {
        return AllowListRuleType.class.isAssignableFrom(kv.getProperty().getPropertyType());
    }

    @Override
    public String getJdbcFieldType(JdbcAgent agent, EntityProvider<?> entity, PropertyValue kv) {
        return "VARCHAR(255)";
    }

    @Override
    public Serializable mapToPojoFieldValue(JdbcAgent agent, JdbcMapContext mapContext) {
        if (mapContext.getJdbcFieldValue() instanceof String) {
            AllowListRuleType type = AllowListRuleType.ALL_TYPES.get((String) mapContext.getJdbcFieldValue());
            if (type == null) {
                throw new IllegalArgumentException("unknown ruleType: " + mapContext.getJdbcFieldValue());
            }
            return type;
        }
        throw new IllegalArgumentException("not support");
    }

    @Override
    public Object mapToJdbcFieldValue(JdbcAgent agent, EntityProvider<?> entity, Map.Entry<String, PropertyValue> pojoState) {
        Object value = pojoState.getValue().getValue();
        return value instanceof AllowListRuleType ? ((AllowListRuleType) value).name() : null;
    }
}
