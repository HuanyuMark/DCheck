package org.example.dcheck.api;

import lombok.Value;
import lombok.experimental.NonFinal;

/**
 * Date: 2025/4/2
 *
 * @author 三石而立Sunsy
 */
@Value
@NonFinal
public class AddRuleContext {
    AllowListRule rule;
    boolean enabled;
    int order;
}
