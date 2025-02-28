package org.example.dcheck.api;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.Collection;
import java.util.Map;

/**
 * Date 2025/02/28
 *
 * @author 三石而立Sunsy
 */
@Data
@Builder
public class MetadataMatchCondition {
    @Singular
    private final Map<String, String> eqs;
    @Singular
    private final Map<String, Collection<String>> ins;
}
