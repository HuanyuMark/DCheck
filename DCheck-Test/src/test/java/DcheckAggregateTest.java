import org.example.dcheck.api.*;
import org.example.dcheck.impl.DocxDocument;
import org.example.dcheck.impl.UnknownDocument;
import org.example.dcheck.spi.DuplicateCheckingProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Arrays;
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
    public void quickStart() throws IOException {
        // 获取查重入口类实例（spi机制）
        DuplicateChecking checking = DuplicateCheckingProvider.getInstance().getChecking();
        // 可以选择一个合适的时间初始化，也可以不手动调用。
        // 但是要注意该方法较为耗时。如果不提前初始化，会在第一次调用其他api时耗时很多
        checking.init();

        // 临时构建文档集合（从中查找重复内容）
        // 使用临时集合不适合需要持久化的场景，见 quickStartDurable
        List<Document> tempDiffCollection;

        try (Stream<Path> stream = Files.list(Paths.get("diff-files"))) {
            tempDiffCollection = stream
                    .map(p -> new AbstractMap.SimpleEntry<>(p, (Supplier<InputStream>) () -> {
                        try {
                            return Files.newInputStream(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }))
                    .map(e -> new UnknownDocument(e.getKey().toString(), (TextContent) () -> e.getValue().get()))
                    .collect(Collectors.toList());
        }

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

        checkResult.getRelevantDocuments().forEach(doc->{
            System.out.println("docId: "+doc.getDocumentId()+" score: "+doc.getScore());
        });
        for (int i = 0; i < checkResult.getRelevantParagraphs().size(); i++) {
            int finalI = i;
            checkResult.getRelevantParagraphs().get(i).forEach(paragraph->{
                System.out.println("paragraph of doc '"+ finalI +"': "+paragraph.getDocumentId()+" relevancy: "+paragraph.getRelevancy()+" location: "+paragraph.getLocation());
            });
        }
    }

    @Test
    public void quickStartDurable() throws IOException {
        // 获取查重入口类实例（spi机制）
        DuplicateChecking checking = DuplicateCheckingProvider.getInstance().getChecking();
        // 可以选择一个合适的时间初始化，也可以不手动调用。
        // 但是要注意该方法较为耗时。如果不提前初始化，会在第一次调用其他api时耗时很多
        checking.init();

        // 临时构建文档集合（从中查找重复内容）
        // 使用临时集合不适合需要持久化的场景，见 quickStartDurable
        List<Document> diffCollection;

        try (Stream<Path> stream = Files.list(Paths.get("diff-files"))) {
            diffCollection = stream
                    .map(p -> new AbstractMap.SimpleEntry<>(p, (Supplier<InputStream>) () -> {
                        try {
                            return Files.newInputStream(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }))
                    .map(e -> new UnknownDocument(e.getKey().toString(), (TextContent) () -> e.getValue().get()))
                    .collect(Collectors.toList());
        }

        // 选择第一个文档（可以不存在于集合中）
        // 如果你需要互相对比集合中各个文档。建议把这些文档都加入到该集合，节省性能
        Document diffTarget = diffCollection.get(0);

        // 在这里使用一个 collectionId 复用一个集合，该集合是持久化的
        DocumentCollection collection = checking.getRelevancyEngine().getOrCreateDocumentCollection("test");
        collection.addDocument(diffCollection);

        CheckResult checkResult = checking.check(
                Check.builder()
                        .document(diffTarget)
                        .topKOfDocument(2)
                        .topKOfEachParagraph(5)
                        .build(),
                collection);

        checkResult.getRelevantDocuments().forEach(doc->{
            System.out.println("docId: "+doc.getDocumentId()+" score: "+doc.getScore());
        });
        for (int i = 0; i < checkResult.getRelevantParagraphs().size(); i++) {
            int finalI = i;
            checkResult.getRelevantParagraphs().get(i).forEach(paragraph->{
                System.out.println("paragraph of doc '"+ finalI +"': "+paragraph.getDocumentId()+" relevancy: "+paragraph.getRelevancy()+" location: "+paragraph.getLocation());
            });
        }
    }
}
