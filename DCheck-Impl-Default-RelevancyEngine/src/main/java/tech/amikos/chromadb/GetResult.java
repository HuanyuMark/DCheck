package tech.amikos.chromadb;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Date 2025/03/03
 *
 * @author 三石而立Sunsy
 */
@Data
public class GetResult {
    private List<String> documents;
    private List<List<Float>> embeddings;
    private List<String> ids;
    private List<Map<String, Object>> metadatas;
}