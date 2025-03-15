package org.example.dcheck.api;

/**
 * Date 2025/02/26
 * an interface represent a stage of RAG.
 * it can be used in the relevancy engine based on the embedding function.
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface Reranker extends DCheckComponent {

    Reranker NOP = (relevancyResult, query) -> relevancyResult;

    ParagraphRelevancyQueryResult rerank(ParagraphRelevancyQueryResult relevancyResult, ParagraphRelevancyQuery query);

}
