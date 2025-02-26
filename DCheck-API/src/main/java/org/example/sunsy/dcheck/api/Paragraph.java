package org.example.sunsy.dcheck.api;

import java.io.InputStream;
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

    InputStream getInputStream();

    ParagraphLocation getLocation();

    Map<String, Object> getMetadata();
}
