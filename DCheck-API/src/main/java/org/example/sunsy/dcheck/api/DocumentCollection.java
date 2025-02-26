package org.example.sunsy.dcheck.api;

import java.util.List;

/**
 * Date 2025/02/25
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface DocumentCollection {
    String getId();

    void addDocument(List<DocumentCreation> creations);

    void deleteDocument(DocumentDelete delete);

    void drop();

    boolean isExists();
}
