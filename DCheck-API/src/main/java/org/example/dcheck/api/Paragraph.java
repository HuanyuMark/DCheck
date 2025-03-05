package org.example.dcheck.api;

/**
 * Date 2025/02/25
 * the small segment of a document. the basic unit to diff duplicate
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface Paragraph {
    DocumentCollection getCollection();

    Content getContent();

    ParagraphMetadata getMetadata();
}
