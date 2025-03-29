package org.example.dcheck.api;

import java.util.List;
import java.util.Set;

/**
 * Date 2025/02/25
 * a collection contain all paragraph of a group document.
 * use the collection to mark the duplicate check range
 *
 * @author 三石而立Sunsy
 */
public interface DocumentCollection {

    String getId();

    void addDocument(List<Document> creations);

    void deleteDocument(Set<String> documentIds);

    List<Boolean> hasDocument(List<String> documentIds);

    void drop();

    boolean isExists();
}
