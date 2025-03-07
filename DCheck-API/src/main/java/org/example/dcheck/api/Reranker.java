package org.example.dcheck.api;

/**
 * Date 2025/02/26
 * an interface represent a stage of RAG.
 * it can be used in the relevancy engine based on the embedding function.
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface Reranker {

    default void init() throws Exception {
    }

    ParagraphRelevancyQueryResult rerank(ParagraphRelevancyQueryResult relevancyResult, ParagraphRelevancyQuery query);

    Reranker NOP = (relevancyResult, query) -> relevancyResult;

}
