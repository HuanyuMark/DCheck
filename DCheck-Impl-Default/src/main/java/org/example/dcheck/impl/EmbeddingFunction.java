package org.example.dcheck.impl;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;

import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
public interface EmbeddingFunction extends tech.amikos.chromadb.embeddings.EmbeddingFunction {
    void init() throws Exception;

    List<Embedding> embedUnknownTypeDocuments(List<Supplier<InputStream>> documents) throws EFException;
}
