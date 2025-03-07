package org.example.dcheck.impl;

import lombok.Data;
import org.example.dcheck.api.BuiltinDocumentType;
import org.example.dcheck.api.Content;
import org.example.dcheck.api.Document;
import org.example.dcheck.api.DocumentType;

/**
 * Date 2025/02/27
 *
 * @author 三石而立Sunsy
 */
@Data
public class PdfDocument implements Document {
    private final String id;
    private final Content content;
    private final DocumentType documentType = BuiltinDocumentType.PDF;
}
