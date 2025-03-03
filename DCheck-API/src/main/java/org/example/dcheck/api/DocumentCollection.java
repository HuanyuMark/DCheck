package org.example.dcheck.api;

import java.util.List;

/**
 * Date 2025/02/25
 * a collection contain all paragraph of a group document.
 * use the collection to mark the duplicate check range
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface DocumentCollection {
    String getId();

    void addDocument(List<Document> creations);

    void deleteDocument(List<String> documentIds);

    default List<Boolean> hasDocument(List<String> documentIds) {
        //TODO implement in implementation class...
        throw new UnsupportedOperationException();
    }

    void drop();

    boolean isExists();
}
