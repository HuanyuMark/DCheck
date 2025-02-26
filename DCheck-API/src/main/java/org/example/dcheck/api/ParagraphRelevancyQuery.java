package org.example.dcheck.api;

import java.util.Set;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface ParagraphRelevancyQuery {
    String getCollectionId();

    Content getParagraph();

    int getTopK();

    Set<String> getIncludeMetadata();
}
