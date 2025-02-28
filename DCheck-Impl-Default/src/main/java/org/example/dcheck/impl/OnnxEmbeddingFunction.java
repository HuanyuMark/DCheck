package org.example.dcheck.impl;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;

import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

/**
 * Date 2025/02/28
 *
 * @author 三石而立Sunsy
 */
public class OnnxEmbeddingFunction implements EmbeddingFunction {
    @Override
    public void init() throws Exception {

    }

    @Override
    public List<Embedding> embedUnknownTypeDocuments(List<Supplier<InputStream>> documents) throws EFException {
        return null;
    }

    @Override
    public Embedding embedQuery(String query) throws EFException {
        return null;
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws EFException {
        return null;
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws EFException {
        return null;
    }
}
