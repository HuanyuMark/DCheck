import org.example.dcheck.api.embedding.Embedding;
import org.example.dcheck.api.embedding.EmbeddingFunction;
import org.example.dcheck.impl.ChromaEmbeddingFunctionWrapper;
import org.junit.jupiter.api.Test;
import tech.amikos.chromadb.Client;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.handler.ApiException;

import java.util.Collections;
import java.util.List;

/**
 * Date: 2025/3/6
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("all")
public class TestApp {

    @Test
    public void testChromaWrapper() throws EFException {
        Object obj = ChromaEmbeddingFunctionWrapper.wrap(new MyFunc());
        System.out.println(obj);
    }

    @Test
    public void testChroma() throws ApiException {
        Client c = new Client("http://localhost:8000");
        c.heartbeat();
        String collectionName = "temp\0ffff";
        c.createCollection(collectionName, Collections.emptyMap(), true, ChromaEmbeddingFunctionWrapper.wrap
                (new EmbeddingFunction() {
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
                }));
        c.deleteCollection(collectionName);
    }

    static class MyFunc implements EmbeddingFunction {
        @Override
        public void init() throws Exception {

        }

        @Override
        public Embedding embedQuery(String query) throws Exception {
            return Embedding.from(new float[]{1f, 2f, 3f});
        }

        @Override
        public List<Embedding> embedDocuments(List<String> documents) throws Exception {
            return null;
        }

        @Override
        public List<Embedding> embedDocuments(String[] documents) throws Exception {
            return null;
        }
    }
}
