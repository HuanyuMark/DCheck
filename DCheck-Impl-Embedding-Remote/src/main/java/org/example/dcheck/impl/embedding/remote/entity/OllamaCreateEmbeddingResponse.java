package org.example.dcheck.impl.embedding.remote.entity;

import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
public class OllamaCreateEmbeddingResponse {
    @NonNull
    public final String model;
    @NonNull
    public final List<List<Float>> embeddings;
}
