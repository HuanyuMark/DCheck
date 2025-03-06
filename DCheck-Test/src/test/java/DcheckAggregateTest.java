import org.example.dcheck.api.*;
import org.example.dcheck.impl.DocxDocument;
import org.example.dcheck.spi.DuplicateCheckingProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import tech.amikos.chromadb.Client;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;
import tech.amikos.chromadb.handler.ApiException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Date 2025/02/28
 * 正在编写中
 *
 * @author 三石而立Sunsy
 */
public class DcheckAggregateTest {

    @Test
    public void quickStart() throws Exception {
        DuplicateChecking checking = getDuplicateChecking();

        // 临时构建文档集合（从中查找重复内容）
        // 使用临时集合不适合需要持久化的场景，见 quickStartDurable
        List<Document> tempDiffCollection = getDocuments();

        // 选择第一个文档（可以不存在于集合中）
        // 如果你需要互相对比集合中各个文档。建议把这些文档都加入到该集合，节省性能
        Document diffTarget = tempDiffCollection.get(0);

        //多次对同一个tempDiffCollection调用check方法，会造成较大的性能损耗，如果需要复用一个tempDiffCollection
        // 请参照 quickStartDurable() 用例
        CheckResult checkResult = checking.check(
                Check.builder()
                        .document(diffTarget)
                        .topKOfDocument(2)
                        .topKOfEachParagraph(5)
                        .build(),
                tempDiffCollection);

        print(checkResult);

        checking.close();
    }

    @Test
    public void quickStartDurable() throws Exception {
        // 获取查重入口类实例（spi机制）
        DuplicateChecking checking = getDuplicateChecking();

        List<Document> diffCollection = getDocuments();

        // 选择第一个文档（可以不存在于集合中）
        // 如果你需要互相对比集合中各个文档。建议把这些文档都加入到该集合，节省性能
        Document diffTarget = diffCollection.get(0);

        // 在这里使用一个 collectionId 复用一个集合，该集合是持久化的
        DocumentCollection collection = checking.getRelevancyEngine().getOrCreateDocumentCollection("test");
        collection.addDocument(diffCollection);

        // 你可以在

        CheckResult checkResult = checking.check(
                Check.builder()
                        .document(diffTarget)
                        .topKOfDocument(2)
                        .topKOfEachParagraph(5)
                        .build(),
                collection);

        print(checkResult);

        checking.close();
    }

    @NotNull
    private static List<Document> getDocuments() throws IOException {
        // 临时构建文档集合（从中查找重复内容）
        // 使用临时集合不适合需要持久化的场景，见 quickStartDurable
        List<Document> diffCollection;
        Path input = Paths.get("diff-files");
        try (Stream<Path> stream = Files.list(input)) {
            diffCollection = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().equals("doc5.pdf"))
                    .map(p -> new AbstractMap.SimpleEntry<>(p, (Supplier<InputStream>) () -> {
                        try {
                            return Files.newInputStream(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }))
                    .map(e -> new DocxDocument(e.getKey().toString(), (TextContent) () -> e.getValue().get()))
                    .collect(Collectors.toList());
            if (diffCollection.isEmpty()) {
                throw new IllegalStateException("请将查重文件放入到目标目录中，目标目录中没有文件！Please place the duplicate checking files into the target directory. The target directory is empty!\n" +
                        "目标目录/target directory: " + input.toAbsolutePath());
            }
        }
        return diffCollection;
    }

    @NotNull
    private static DuplicateChecking getDuplicateChecking() {
        DuplicateChecking checking = DuplicateCheckingProvider.getInstance().getChecking();
        // 可以选择一个合适的时间初始化，也可以不手动调用。
        // 但是要注意该方法较为耗时。如果不提前初始化，会在第一次调用其他api时耗时很多
        checking.init();
        return checking;
    }

    private static void print(CheckResult checkResult) {
        checkResult.getRelevantDocuments().forEach(doc -> System.out.println("docId: " + doc.getDocumentId() + " score: " + doc.getScore()));
        for (int i = 0; i < checkResult.getRelevantParagraphs().size(); i++) {
            int finalI = i;
            checkResult.getRelevantParagraphs().get(i).forEach(paragraph -> System.out.println("paragraph of doc '" + finalI + "': " + paragraph.getMetadata().getDocumentId() + " relevancy: " + paragraph.getRelevancy() + " location: " + paragraph.getMetadata().getLocation()));
        }
    }

    @Test
    public void testChroma() throws ApiException {
        Client c = new Client("http://localhost:8000");
        c.heartbeat();
        String collectionName = "temp\0ffff";
        c.createCollection(collectionName, Collections.emptyMap(), true, new EmbeddingFunction() {
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
        });
        c.deleteCollection(collectionName);
    }
}
