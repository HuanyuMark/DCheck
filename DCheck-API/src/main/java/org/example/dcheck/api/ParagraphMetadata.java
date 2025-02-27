package org.example.dcheck.api;

import lombok.var;
import org.springframework.cglib.beans.BeanMap;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Date 2025/02/27
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface ParagraphMetadata extends Map<String, Object> {

    String getDocumentId();

    ParagraphType getParagraphType();

    ParagraphLocation getLocation();

    default Map<String, String> toFlatMap(Function<Object, String> jsonSerializer) {
        BeanMap beanMap = BeanMap.create(this);
        var res = new HashMap<String, String>((int) Math.ceil(beanMap.size() / 0.75f));
        for (Object key : beanMap.keySet()) {
            if (!(key instanceof CharSequence)) continue;
            Object value = beanMap.get(key);
            if (value == null) {
                res.put(key.toString(), null);
                continue;
            }
            res.put(key.toString(), value instanceof CharSequence ? value.toString() : jsonSerializer.apply(value));
        }
        return res;
    }
}
