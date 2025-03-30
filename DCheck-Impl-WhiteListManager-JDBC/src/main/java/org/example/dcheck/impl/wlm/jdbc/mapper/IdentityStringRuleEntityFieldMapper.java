package org.example.dcheck.impl.wlm.jdbc.mapper;

import lombok.Getter;
import org.example.dcheck.api.PojoField;
import org.example.dcheck.api.WhiteListRule;
import org.example.dcheck.impl.wlm.jdbc.api.RuleEntityFieldMapper;

import java.io.Serializable;
import java.util.Properties;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
@Getter
public class IdentityStringRuleEntityFieldMapper implements RuleEntityFieldMapper {
    protected int stringFieldLength;
    private String jdbcFieldType;

    public IdentityStringRuleEntityFieldMapper() {
        setStringFieldLength(255);
    }

    public void setStringFieldLength(int stringFieldLength) {
        if (stringFieldLength <= 0) {
            throw new IllegalArgumentException("stringFieldLength must be > 0");
        }
        this.stringFieldLength = stringFieldLength;
        jdbcFieldType = "VARCHAR(" + stringFieldLength + ")";
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
        return jdbcFieldType;
    }
}
