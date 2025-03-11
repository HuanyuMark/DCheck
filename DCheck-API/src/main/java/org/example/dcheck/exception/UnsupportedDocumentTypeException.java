package org.example.dcheck.exception;

import lombok.experimental.StandardException;
import org.example.dcheck.api.DocumentType;

/**
 * Date: 2025/3/12
 *
 * @author 三石而立Sunsy
 */
@StandardException
public class UnsupportedDocumentTypeException extends DCheckRuntimeException {
    public UnsupportedDocumentTypeException(DocumentType type) {
        this("Document type not support: " + type);
    }
}
