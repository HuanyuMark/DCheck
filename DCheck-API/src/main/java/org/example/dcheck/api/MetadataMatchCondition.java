package org.example.dcheck.api;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.var;

import java.util.Collection;
import java.util.HashSet;
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

    public void validate() throws IllegalArgumentException {
        var uniqueFields = new HashSet<String>(eqs.keySet());
        for (String field : ins.keySet()) {
            if (uniqueFields.add(field)) {
                continue;
            }
            throw new IllegalArgumentException("field cannot apply $eq and $in statements both: " + field);
        }
    }
}
