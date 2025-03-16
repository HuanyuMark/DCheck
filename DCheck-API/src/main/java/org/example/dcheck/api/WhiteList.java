package org.example.dcheck.api;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

/**
 * Date: 2025/3/17
 * TODO add support to filter that word in white list...
 *
 * @author 三石而立Sunsy
 */
@Value
@NonNull
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WhiteList {
    @EqualsAndHashCode.Include
    String id;


}
