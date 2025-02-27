package org.example.dcheck.impl;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Date 2025/02/27
 *
 * @author 三石而立Sunsy
 */
public class SerializerSupport {
    @Getter
    @Setter
    @NonNull
    private static Gson gson = new Gson();
}
