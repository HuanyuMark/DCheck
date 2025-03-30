package org.example.dcheck.api;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Type;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
@Data
public class PojoField {
    private final String fieldName;
    private final Type fieldType;
    @Nullable
    private final Serializable value;
}
