package org.example.dcheck.impl;

import org.example.dcheck.api.Document;
import org.example.dcheck.api.DocumentCollection;

import java.util.List;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public class MilvusDocumentCollection implements DocumentCollection {
    @Override
    public String getId() {
        return null;
    }


    @Override
    public void addDocument(List<Document> creations) {

    }

    @Override
    public void deleteDocument(List<String> documentIds) {

    }

    @Override
    public void drop() {

    }

    @Override
    public boolean isExists() {
        return false;
    }
}
