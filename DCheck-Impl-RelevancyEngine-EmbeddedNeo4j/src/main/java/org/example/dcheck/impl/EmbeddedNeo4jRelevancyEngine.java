package org.example.dcheck.impl;

import org.example.dcheck.api.*;

/**
 * Date: 2025/2/28
 *
 * @author 三石而立Sunsy
 */
public class EmbeddedNeo4jRelevancyEngine implements ParagraphRelevancyEngine {

    @Override
    public void init() {

    }

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
    public DocumentCollection getOrCreateDocumentCollection(String id) {
        return null;
    }

    @Override
    public void removeDocumentCollection(String collectionId) {

    }

    @Override
    public TempDocumentCollection newTempDocumentCollection() {
        return null;
    }
}
