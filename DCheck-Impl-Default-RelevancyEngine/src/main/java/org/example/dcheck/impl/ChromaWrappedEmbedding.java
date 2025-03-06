package org.example.dcheck.impl;

import tech.amikos.chromadb.Embedding;

import java.util.List;

/**
 * Date: 2025/3/3
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public class ChromaWrappedEmbedding extends Embedding {

    public static ChromaWrappedEmbedding wrap(org.example.dcheck.api.embedding.Embedding target) {
        return new ChromaWrappedEmbedding(target);
    }

    protected final org.example.dcheck.api.embedding.Embedding target;

    public ChromaWrappedEmbedding(org.example.dcheck.api.embedding.Embedding target) {
        super(target.asArray());
        this.target = target;
    }

    @Override
    public List<Float> asList() {
        return target.asList();
    }

    @Override
    public int getDimensions() {
        return target.getDimensions();
    }

    @Override
    public float[] asArray() {
        return target.asArray();
    }

    @Override
    public String toString() {
        return target.toString();
    }
}
