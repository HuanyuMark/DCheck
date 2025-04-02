package org.example.dcheck.impl.alm.jdbc.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.dcheck.api.EntityProvider;
import org.example.dcheck.impl.alm.jdbc.support.JdbcAgent;

/**
 * Date 2025/04/02
 *
 * @author 三石而立Sunsy
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleSetEntity {
    String id;
    String description;
    Boolean added;
    public static final String tableName = JdbcAgent.DCHECK_TABLE_PREFIX + "alr_ruleSet";
    public static final EntityProvider<RuleSetEntity> provider = EntityProvider.getDefaultProvider(RuleSetEntity.class, RuleSetEntity::new);
}
