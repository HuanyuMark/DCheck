package org.example.dcheck.api.embedding;

import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
public interface EmbeddingFunction {
    void init() throws Exception;

    Embedding embedQuery(String query) throws Exception;

    List<Embedding> embedDocuments(List<String> documents) throws Exception;

    List<Embedding> embedDocuments(String[] documents) throws Exception;

    List<Embedding> embedUnknownTypeDocuments(List<Supplier<InputStream>> documents) throws Exception;
}
