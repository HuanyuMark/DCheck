package org.example.dcheck.impl.embedding.remote.entity;

import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
public class OllamaCreateEmbeddingRequest {
    @NonNull
    private final String model;
    @NonNull
    private final List<String> input;
}
