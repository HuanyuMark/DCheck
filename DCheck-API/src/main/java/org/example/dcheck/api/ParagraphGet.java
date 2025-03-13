package org.example.dcheck.api;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2025/3/10
 *
 * @author 三石而立Sunsy
 */
@Data
@Builder
public class ParagraphGet {
    @NonNull
    private final String collectionId;
    @Nullable
    private final Integer maxCount;
    @Nullable
    private final MetadataMatchCondition condition;
}
