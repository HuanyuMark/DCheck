package org.example.sunsy.dcheck.api;

import java.io.InputStream;

/**
 * Date 2025/02/25
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface Document {
    String getId();

    DocumentType getDocumentType();

    InputStream getContent();
}
