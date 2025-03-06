import lombok.var;
import org.example.dcheck.api.embedding.Embedding;
import org.example.dcheck.api.embedding.EmbeddingFunction;
import org.example.dcheck.impl.ChromaEmbeddingFunctionWrapper;
import org.junit.jupiter.api.Test;
import tech.amikos.chromadb.EFException;

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
        var obj = ChromaEmbeddingFunctionWrapper.wrap(new MyFunc());
        System.out.println(obj);
    }

    static class MyFunc implements EmbeddingFunction {
        @Override
        public void init() throws Exception {

        }

        @Override
        public Embedding embedQuery(String query) throws Exception {
            return Embedding.from(new float[]{1f, 2f, 3f}, getName());
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
