package org.example.sunsy.dcheck.api;

import java.io.InputStream;
import java.util.Set;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface ParagraphRelevancyQuery {
    String getCollectionId();

    InputStream getParagraph();

    int getTopK();

    Set<String> getIncludeMetadata();
}
