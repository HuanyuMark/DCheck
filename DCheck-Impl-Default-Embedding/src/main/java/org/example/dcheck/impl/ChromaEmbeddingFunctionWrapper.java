package org.example.dcheck.impl;

import lombok.RequiredArgsConstructor;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Date: 2025/3/3
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
@RequiredArgsConstructor
public class ChromaEmbeddingFunctionWrapper implements EmbeddingFunction {

    public static ChromaEmbeddingFunctionWrapper wrap(org.example.dcheck.api.embedding.EmbeddingFunction target) {
        return new ChromaEmbeddingFunctionWrapper(target);
    }


    protected final org.example.dcheck.api.embedding.EmbeddingFunction target;

    @Override
    public Embedding embedQuery(String query) throws EFException {
        try {
            return ChromaEmbeddingWrapper.wrap(target.embedQuery(query));
        } catch (Throwable e) {
            throw wrapException(e);
        }
    }

    protected EFException wrapException(Throwable e) {
        return e instanceof EFException ? (EFException) e : new EFException(e);
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws EFException {
        try {
            return target.embedDocuments(documents).stream().map(ChromaEmbeddingWrapper::wrap).collect(Collectors.toList());
        } catch (Throwable e) {
            throw wrapException(e);
        }
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws EFException {
        try {
            return target.embedDocuments(documents).stream().map(ChromaEmbeddingWrapper::wrap).collect(Collectors.toList());
        } catch (Throwable e) {
            throw wrapException(e);
        }
    }
}
