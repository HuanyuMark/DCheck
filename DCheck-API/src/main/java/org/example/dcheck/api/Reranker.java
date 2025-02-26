package org.example.dcheck.api;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface Reranker {

    default void init() throws Exception {
    }

    ;

    ParagraphRelevancyQueryResult rerank(ParagraphRelevancyQueryResult relevancyResult, ParagraphRelevancyQuery query);
}
