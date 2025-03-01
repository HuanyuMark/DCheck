package org.example.dcheck.impl;

import org.example.dcheck.embedding.EmbeddingFunction;
import tech.amikos.chromadb.Embedding;

import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

/**
 * Date 2025/02/28
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings({"unused", "unusedThrown"})
public class OnnxEmbeddingFunction implements EmbeddingFunction {
    @Override
    public void init() {

    }

    @Override
    public List<Embedding> embedUnknownTypeDocuments(List<Supplier<InputStream>> documents) {
        return null;
    }

    @Override
    public Embedding embedQuery(String query) {
        return null;
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) {
        return null;
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) {
        return null;
    }
}
