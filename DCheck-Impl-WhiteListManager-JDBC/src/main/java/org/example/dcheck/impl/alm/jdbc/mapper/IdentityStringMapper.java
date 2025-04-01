package org.example.dcheck.impl.alm.jdbc.mapper;

import lombok.Getter;
import org.example.dcheck.api.EntityProvider;
import org.example.dcheck.api.PojoField;
import org.example.dcheck.impl.alm.jdbc.support.JdbcAgent;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
@Getter
public class IdentityStringMapper extends StringMapper {
    /**
     * some db may limit the max length of string field
     */
    protected int stringFieldLength;
    private String jdbcFieldType;

    public IdentityStringMapper() {
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
    protected boolean support(PojoField kv) {
        return "id".equalsIgnoreCase(kv.getProperty().getName()) && kv.getProperty().getPropertyType().isAssignableFrom(String.class);
    }

    @Override
    public String getJdbcFieldType(JdbcAgent agent, EntityProvider<?> entity, PojoField kv) {
        return jdbcFieldType;
    }
}
