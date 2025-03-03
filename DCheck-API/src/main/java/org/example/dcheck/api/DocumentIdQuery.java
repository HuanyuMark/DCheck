package org.example.dcheck.api;

import lombok.*;

import java.util.List;

/**
 * Date 2025/03/03
 *
 * @author 三石而立Sunsy
 */
@Data
@Builder
@AllArgsConstructor
public class DocumentIdQuery {
    @NonNull
    private final String collectionId;
    @NonNull
    @Singular
    private final List<String> documentIds;
}
