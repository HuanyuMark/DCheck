package org.example.dcheck.api;

/**
 * Date 2025/02/25
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface Document {
    String getId();

    DocumentType getDocumentType();

    Content getContent();
}
