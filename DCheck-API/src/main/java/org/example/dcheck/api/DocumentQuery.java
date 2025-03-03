package org.example.dcheck.api;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;

import java.util.List;

/**
 * Date 2025/03/03
 *
 * @author 三石而立Sunsy
 */
@Data
@Builder
public class DocumentQuery {
    @NonNull
    private final String collectionId;
    @NonNull
    @Singular
    private final List<String> documentIds;
}
