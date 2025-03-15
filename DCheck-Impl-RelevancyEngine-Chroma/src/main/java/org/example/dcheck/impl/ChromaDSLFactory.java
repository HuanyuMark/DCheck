package org.example.dcheck.impl;

import org.example.dcheck.api.MetadataMatchCondition;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Date 2025/02/28
 *
 * @author 三石而立Sunsy
 */
public class ChromaDSLFactory {


    @SuppressWarnings("unchecked")
    public static Map<String, Object> where(MetadataMatchCondition condition, Function<Map.Entry<String, Object>, Object> valueMapper) {
        condition.validate();
        Map<String, Object> eqs = condition.getEqs().entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<String, Object>(e.getKey(), Collections.singletonMap("$eq", valueMapper.apply((Map.Entry<String, Object>) ((Object) e)))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, Object> ins = condition.getIns().entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<String, Object>(e.getKey(), Collections.singletonMap("$in", valueMapper.apply((Map.Entry<String, Object>) ((Object) e)))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, Object> nes = condition.getNes().entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<String, Object>(e.getKey(), Collections.singletonMap("$ne", valueMapper.apply((Map.Entry<String, Object>) ((Object) e)))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, Object> where = new HashMap<>(eqs);
        where.putAll(ins);
        where.putAll(nes);
        return where;
    }

    public static Map<String, Object> where(MetadataMatchCondition condition) {
        return where(condition, Map.Entry::getValue);
    }
}
