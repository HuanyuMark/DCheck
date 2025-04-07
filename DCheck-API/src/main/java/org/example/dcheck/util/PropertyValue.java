package org.example.dcheck.util;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
@Data
public class PropertyValue {
    private final BeanProperty property;
    @Nullable
    private final Object value;
}
