package org.example.dcheck.api;

/**
 * Date 2025/02/25
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface Paragraph {
    DocumentCollection getCollection();

    String getDocumentId();

    Content getContent();

    ParagraphLocation getLocation();

    ParagraphType getParagraphType();

    ParagraphMetadata getMetadata();
}
