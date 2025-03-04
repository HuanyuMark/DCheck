package org.example.dcheck.impl;

import org.example.dcheck.api.*;

import java.util.Collections;
import java.util.List;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
public class MilvusParagraphRelevancyEngine implements ParagraphRelevancyEngine {


    @Override
    public ParagraphRelevancyQueryResult queryParagraph(ParagraphRelevancyQuery query) {
        return null;
    }

    @Override
    public void addParagraph(ParagraphRelevancyCreation creation) {

    }

    @Override
    public void removeDocument(DocumentDelete delete) {

    }

    @Override
    public List<Boolean> hasDocument(DocumentIdQuery query) {
        return Collections.emptyList();
    }

    @Override
    public DocumentCollection getOrCreateDocumentCollection(String collectionId) {
        return null;
    }

    @Override
    public void removeDocumentCollection(String collectionId) {

    }

    @Override
    public TempDocumentCollection newTempDocumentCollection() {
        return null;
    }

    @Override
    public void close() {
    }
}
