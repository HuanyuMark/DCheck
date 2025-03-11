package org.example.dcheck.api;

import lombok.Data;

/**
 * Date 2025/03/11
 *
 * @author 三石而立Sunsy
 */
@Data
public class DCheckComponent<T> {
    private final String name;
    private final T component;
}
