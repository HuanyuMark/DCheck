package org.example.dcheck.api;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2025/3/10
 *
 * @author 三石而立Sunsy
 */
@Value
@Builder
@NonFinal
public class ParagraphGet {

    @NonNull
    String collectionId;

    @Nullable
    Integer maxCount;

    @Nullable
    MetadataMatchCondition condition;
}
