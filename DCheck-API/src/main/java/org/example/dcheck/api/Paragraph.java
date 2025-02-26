package org.example.dcheck.api;

import java.util.Map;

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

    Map<String, Object> getMetadata();

    ParagraphType getParagraphType();
}
