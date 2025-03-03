package org.example.dcheck.api;

import java.util.List;

/**
 * Date 2025/02/25
 * 相似度引擎，负责执行与相似度计算相关的所有核心逻辑
 * the core api to do duplicate checking
 * @see DuplicateChecking
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface ParagraphRelevancyEngine {
    ParagraphRelevancyQueryResult queryParagraph(ParagraphRelevancyQuery query);

    void addParagraph(ParagraphRelevancyCreation creation);

    void removeDocument(DocumentDelete delete);

    List<Boolean> hasDocument(DocumentIdQuery query);

    DocumentCollection getOrCreateDocumentCollection(String collectionId);

    void removeDocumentCollection(String collectionId);

    TempDocumentCollection newTempDocumentCollection();

    default void init() {
    }
}
