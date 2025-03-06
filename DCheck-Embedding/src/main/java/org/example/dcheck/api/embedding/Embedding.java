package org.example.dcheck.api.embedding;

import lombok.Getter;
import lombok.var;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
public class Embedding {
    private final float[] embedding;

    private final String embeddingFunction;

    protected static final int TRUNCATE_COUNT = 30;

    public Embedding(float[] embeddings, String embeddingFunction) {
        this.embedding = embeddings;
        this.embeddingFunction = embeddingFunction;
    }

    public Embedding(List<? extends Number> embedding, String embeddingFunction) {
        this.embedding = new float[embedding.size()];
        this.embeddingFunction = embeddingFunction;
        for (int i = 0; i < embedding.size(); i++) {
            //TODO what if embeddings are integers?
            this.embedding[i] = embedding.get(i).floatValue();
        }
    }


    public List<Float> asList() {
        return IntStream.range(0, embedding.length)
                .mapToObj(i -> embedding[i])
                .collect(Collectors.toList());

    }

    public int getDimensions() {
        return embedding.length;
    }

    public float[] asArray() {
        return embedding;
    }

    public static Embedding from(List<Float> embedding, String embeddingFunction) {
        return new Embedding(embedding, embeddingFunction);
    }

    public static Embedding from(float[] embedding, String embeddingFunction) {
        return new Embedding(embedding, embeddingFunction);
    }

    @Override
    public String toString() {
        var b = new StringBuilder("Embedding(");
        for (int i = 0; i < embedding.length && i <= TRUNCATE_COUNT; i++) {
            b.append(embedding[i]).append(',');
        }
        int last = b.length() - 1;
        if (b.charAt(last) == ',') {
            b.deleteCharAt(last);
        }
        b.append(')');
        return b.toString();
    }
}
