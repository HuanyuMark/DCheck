package org.example.sunsy.dcheck.api;


import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Map;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface ParagraphRelevancyCreation {
    @Nullable
    String getCollectionId();

    InputStream getParagraph();

    Map<String, Object> getMetadata();
}
