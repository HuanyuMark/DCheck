package org.example.dcheck.impl;

import lombok.var;
import org.example.dcheck.api.MetadataMatchCondition;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Date 2025/02/28
 *
 * @author 三石而立Sunsy
 */
public class ChromaDSLFactory {

    public static Map<String, Object> where(MetadataMatchCondition condition) {
        condition.validate();
        var eqs = condition.getEqs().entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<String, Object>(e.getKey(), Collections.singletonMap("$eq", e.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        var ins = condition.getIns().entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<String, Object>(e.getKey(), Collections.singletonMap("$in", e.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        var nes = condition.getNes().entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<String, Object>(e.getKey(), Collections.singletonMap("$ne", e.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        var where = new HashMap<>(eqs);
        where.putAll(ins);
        where.putAll(nes);
        return where;
    }
}
