package org.example.dcheck.api;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Date 2025/02/28
 *
 * @author 三石而立Sunsy
 */
@Value
@NonFinal
@Builder(toBuilder = true)
public class MetadataMatchCondition {

    /**
     * equal
     */
    @Singular
    Map<String, String> eqs;

    /**
     * in
     */
    @Singular
    Map<String, Set<String>> ins;

    /**
     * not equal
     */
    @Singular
    Map<String, String> nes;

    /**
     * not in
     */
    @Singular
    Map<String, Set<String>> nins;

    /**
     * before apply the condition call this method
     */
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
