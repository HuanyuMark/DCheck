package org.example.dcheck.api;

/**
 * Date 2025/02/25
 * the real document wrapper. associate with needed infos
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface Document {
    String getId();

    default DocumentType getDocumentType() {
        return BuiltinDocumentType.UNKNOWN;
    }

    Content getContent();
}
