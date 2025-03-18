package org.example.dcheck.api;

import lombok.*;
import lombok.experimental.NonFinal;

import java.util.List;

/**
 * Date 2025/03/03
 *
 * @author 三石而立Sunsy
 */
@Value
@Builder
@NonFinal
@AllArgsConstructor
public class DocumentIdQuery {
    @NonNull
    String collectionId;
    @NonNull
    @Singular
    List<String> documentIds;
}
