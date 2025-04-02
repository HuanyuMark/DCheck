package org.example.dcheck.api;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Date: 2025/3/17
 * 在查重正式开始前，需要先借助该实例，过滤一遍所要查重的文档，命中或者与白名单中关键字相似的片段不参与查重
 * rule set storage
 *
 * @author 三石而立Sunsy
 */
public interface AllowListManager extends DCheckComponent {

    @Nullable
    AllowListRuleSet getRuleSet(String ruleSetId);

    @NotNull
    AllowListRuleSet getOrCreateRuleSet(String ruleSetId);

    void removeRuleSet(String ruleSetId);

    @NotNull
    List<AllowListRuleSet> getAllRuleSets();
}
