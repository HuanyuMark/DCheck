package org.example.dcheck.impl.wlm.jdk;

import org.example.dcheck.api.AllowListManager;
import org.example.dcheck.api.AllowListRuleSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
public class JdkAllowListManager implements AllowListManager {
    @Override
    public AllowListRuleSet getRuleSet(String ruleSetId) {
        return null;
    }

    @Override
    public @NotNull AllowListRuleSet getOrCreateRuleSet(String ruleSetId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeRuleSet(String ruleSetId) {

    }

    @Override
    public @NotNull List<AllowListRuleSet> getAllRuleSets() {
        return Collections.emptyList();
    }
}
