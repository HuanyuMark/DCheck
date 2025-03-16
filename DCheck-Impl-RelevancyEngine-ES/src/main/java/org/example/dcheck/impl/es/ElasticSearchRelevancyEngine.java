package org.example.dcheck.impl.es;

import org.example.dcheck.api.*;

import java.util.List;

/**
 * Date: 2025/3/16
 *
 * @author 三石而立Sunsy
 */
public class ElasticSearchRelevancyEngine implements ParagraphRelevancyEngine {

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
        return null;
    }

    @Override
    public List<Paragraph> getParagraphs(ParagraphGet query) {
        return null;
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
}
