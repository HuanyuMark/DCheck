package org.example.dcheck.api;

import lombok.NonNull;

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
public interface ParagraphMetadata {

    @NonNull
    String getDocumentId();

    @NonNull
    ParagraphType getParagraphType();

    @NonNull
    ParagraphLocation getLocation();

    Map<String, Object> all();

    default Map<String, String> toFlatMap(Function<Object, String> jsonSerializer) {
        Map<String, String> res = new HashMap<>((int) Math.ceil(all().size() / 0.75f));
        for (String key : all().keySet()) {
            Object value = all().get(key);
            if (value == null) {
                res.put(key, null);
                continue;
            }
            res.put(key, jsonSerializer.apply(value));
        }
        return res;
    }
}
