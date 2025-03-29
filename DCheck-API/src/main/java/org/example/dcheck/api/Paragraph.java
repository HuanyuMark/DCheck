package org.example.dcheck.api;

/**
 * Date 2025/02/25
 * the small segment of a document. the basic unit to diff duplicate
 *
 * @author 三石而立Sunsy
 */
public interface Paragraph {

    Content getContent();

    ParagraphMetadata getMetadata();
}
