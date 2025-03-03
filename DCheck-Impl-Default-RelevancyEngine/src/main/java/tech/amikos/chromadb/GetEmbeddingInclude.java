package tech.amikos.chromadb;

import tech.amikos.chromadb.model.AnyOfGetEmbeddingIncludeItems;

/**
 * Date 2025/03/03
 *
 * @author 三石而立Sunsy
 */
public enum GetEmbeddingInclude implements AnyOfGetEmbeddingIncludeItems {
    EMBEDDINGS,
    METADATAS,
    DOCUMENTS
}
