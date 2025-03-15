package org.example.dcheck.api;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Date 2025/02/28
 *
 * @author 三石而立Sunsy
 */
@Data
@Builder(toBuilder = true)
public class MetadataMatchCondition {
    // equal
    @Singular
    private final Map<String, String> eqs;
    // in
    @Singular
    private final Map<String, Set<String>> ins;
    // not equal
    @Singular
    private final Map<String, String> nes;
    // not in
    @Singular
    private final Map<String, Set<String>> nins;

    public void validate() throws IllegalArgumentException {
        Set<String> uniqueFields = new HashSet<>(eqs.keySet());
        for (String field : ins.keySet()) {
            if (uniqueFields.add(field)) {
                continue;
            }
            throw new IllegalArgumentException("field cannot apply $eq and $in statements both: " + field);
        }
    }
}
