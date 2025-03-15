package org.example.dcheck.api;

import lombok.NonNull;
import org.springframework.cglib.beans.BeanMap;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Date 2025/02/27
 * strict constraint type of paragraph metadata.
 * list the common metadata of paragraph.
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface ParagraphMetadata extends Map<String, Object> {
    @NonNull
    String getDocumentId();

    @NonNull
    ParagraphType getParagraphType();

    @NonNull
    ParagraphLocation getLocation();

    default Map<String, String> toFlatMap(Function<Object, String> jsonSerializer) {
        BeanMap beanMap = BeanMap.create(this);
        Map<String, String> res = new HashMap<>((int) Math.ceil(beanMap.size() / 0.75f));
        for (Object key : beanMap.keySet()) {
            if (!(key instanceof CharSequence)) continue;
            Object value = beanMap.get(key);
            if (value == null) {
                res.put(key.toString(), null);
                continue;
            }
            res.put(key.toString(), jsonSerializer.apply(value));
        }
        return res;
    }
}
