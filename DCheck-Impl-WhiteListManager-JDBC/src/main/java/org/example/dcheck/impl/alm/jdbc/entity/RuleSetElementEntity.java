package org.example.dcheck.impl.alm.jdbc.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.dcheck.api.EntityProvider;
import org.example.dcheck.impl.alm.jdbc.annotation.Index;
import org.example.dcheck.impl.alm.jdbc.support.JdbcAgent;
import org.example.dcheck.util.BeanProperty;
import org.springframework.core.Ordered;

/**
 * Date 2025/04/02
 *
 * @author 三石而立Sunsy
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleSetElementEntity {
    String id;
    @Index
    String ruleSetId;
    @Index
    String ruleId;
    Boolean enabled;
    Integer order;

    public RuleSetElementEntity(String ruleSetId, String ruleId) {
        this.ruleSetId = ruleSetId;
        this.ruleId = ruleId;
        this.id = ruleSetId + "_" + ruleId;
        this.enabled = Boolean.TRUE;
        this.order = Ordered.LOWEST_PRECEDENCE;
    }

    public static final String tableName = JdbcAgent.DCHECK_TABLE_PREFIX + "alr_ruleSetElement";
    public static final EntityProvider<RuleSetElementEntity> provider = EntityProvider.getDefaultProvider(RuleSetElementEntity.class, RuleSetElementEntity::new);

    @Getter
    static final BeanProperty enabledProperty = provider.getSchema().stream().filter(p -> p.getName().equals("enabled")).findFirst().orElseThrow(IllegalStateException::new);
}
