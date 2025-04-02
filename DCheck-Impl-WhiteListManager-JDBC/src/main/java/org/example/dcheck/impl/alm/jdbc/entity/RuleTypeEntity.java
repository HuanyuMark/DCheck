package org.example.dcheck.impl.alm.jdbc.entity;

import lombok.Data;
import org.example.dcheck.api.AllowListRuleType;
import org.example.dcheck.api.EntityProvider;
import org.example.dcheck.impl.alm.jdbc.support.JdbcAgent;

/**
 * Date 2025/04/02
 *
 * @author 三石而立Sunsy
 */
@Data
public class RuleTypeEntity {
    String id;
    AllowListRuleType ruleType;
    public static final String tableName = JdbcAgent.DCHECK_TABLE_PREFIX + "alr_ruleType";
    public static final EntityProvider<RuleTypeEntity> type = EntityProvider.getDefaultProvider(RuleTypeEntity.class, RuleTypeEntity::new);
}
