package org.example.dcheck.api.embedding;

import lombok.Getter;
import lombok.var;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
public class Embedding {
    protected static final int TRUNCATE_COUNT = 30;
    private final float[] embedding;

    public Embedding(float[] embeddings) {
        this.embedding = embeddings;
    }

    public Embedding(List<? extends Number> embedding) {
        this.embedding = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            //TODO what if embeddings are integers?
            this.embedding[i] = embedding.get(i).floatValue();
        }
    }

    public static Embedding from(List<Float> embedding) {
        return new Embedding(embedding);
    }

    public static Embedding from(float[] embedding) {
        return new Embedding(embedding);
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

    @Override
    public String toString() {
        var b = new StringBuilder(getClass().getSimpleName()).append('(');

        int i;
        for (i = 0; i < embedding.length && i <= TRUNCATE_COUNT; i++) {
            b.append(embedding[i]).append(',');
        }

        int last = b.length() - 1;
        if (b.charAt(last) == ',') {
            b.deleteCharAt(last);
        }

        if (i >= TRUNCATE_COUNT) {
            b.append(" ...");
        }

        b.append(')');
        return b.toString();
    }
}
