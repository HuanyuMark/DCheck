package org.example.dcheck.impl;

import lombok.RequiredArgsConstructor;
import org.example.dcheck.api.DocumentCollection;
import org.example.dcheck.api.DocumentCreation;
import org.example.dcheck.api.DocumentDelete;
import org.example.dcheck.api.ParagraphRelevancyEngine;

import java.util.List;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@RequiredArgsConstructor
public class ChromaDocumentCollection implements DocumentCollection {

    private final ParagraphRelevancyEngine engine;

    @Override
    public String getId() {
        return null;
    }

    @Override
    public void addDocument(List<DocumentCreation> creations) {

    }

    @Override
    public void deleteDocument(DocumentDelete delete) {

    }

    @Override
    public void drop() {

    }

    @Override
    public boolean isExists() {
        return false;
    }
}
