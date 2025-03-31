package org.example.dcheck.api;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
@Data
public class PojoField {
    private final BeanProperty property;
    @Nullable
    private final Serializable value;
}
