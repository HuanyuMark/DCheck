package org.example.dcheck.impl;

import lombok.var;
import org.example.dcheck.api.MetadataMatchCondition;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Date 2025/02/28
 *
 * @author 三石而立Sunsy
 */
public class ChromaDSLFactory {

    public static Map<String, Object> where(MetadataMatchCondition condition) {
        var eqs = condition.getEqs().entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<String, Object>(e.getKey(), Collections.singletonMap("$eq", e.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        var ins = condition.getIns().entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<String, Object>(e.getKey(), Collections.singletonMap("$in", e.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        var where = new HashMap<String, Object>();
        where.putAll(eqs);
        where.putAll(ins);

        // check if eqs and ins has same keys (field name)
        if (where.size() != eqs.size() + ins.size()) {
            Set<String> bigSet;
            Set<String> smallSet;
            if (eqs.size() > ins.size()) {
                bigSet = new HashSet<>(eqs.keySet());
                smallSet = ins.keySet();
            } else {
                bigSet = new HashSet<>(ins.keySet());
                smallSet = eqs.keySet();
            }
            bigSet.retainAll(smallSet);
            throw new IllegalArgumentException("field cannot apply $eq and $in statements both: " + bigSet);
        }
        return where;
    }
}
