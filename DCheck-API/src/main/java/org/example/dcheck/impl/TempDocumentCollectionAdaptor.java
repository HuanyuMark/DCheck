package org.example.dcheck.impl;

import lombok.Data;
import lombok.experimental.Delegate;
import org.example.dcheck.api.DocumentCollection;
import org.example.dcheck.api.TempDocumentCollection;

/**
 * Date 2025/02/27
 *
 * @author 三石而立Sunsy
 */
@Data
public class TempDocumentCollectionAdaptor implements TempDocumentCollection {
    @Delegate
    private final DocumentCollection collection;
}
