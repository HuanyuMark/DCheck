package org.example.dcheck.impl.wlm.jdbc.mapper;

import lombok.Getter;
import org.example.dcheck.api.PojoField;
import org.example.dcheck.api.WhiteListRule;
import org.example.dcheck.impl.wlm.jdbc.api.RuleEntityFieldMapper;

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
public class StringRuleEntityFieldMapper implements RuleEntityFieldMapper {
    protected final Set<String> excludeFields = new HashSet<>();

    public StringRuleEntityFieldMapper() {
        excludeFields.add("id");
    }

    @Override
    public boolean support(WhiteListRule rule, Properties jdbcProperties, PojoField kv) {
        return support(kv);
    }

    protected boolean support(PojoField kv) {
        return "id".equalsIgnoreCase(kv.getFieldName()) && kv.getFieldType() instanceof Class && ((Class<?>) kv.getFieldType()).isAssignableFrom(String.class);
    }

    @Override
    public Serializable mapToPojoFieldValue(Properties jdbcProperties, JdbcMapContext mapContext) {
        if (support(mapContext.getPojoField()) && mapContext.getJdbcFieldValue() instanceof String) {
            return mapContext.getJdbcFieldValue();
        }
        throw new IllegalArgumentException("not support");
    }


    @Override
    public String getJdbcFieldType(WhiteListRule rule, Properties jdbcProperties, PojoField kv) {
        return "TEXT";
    }
}
