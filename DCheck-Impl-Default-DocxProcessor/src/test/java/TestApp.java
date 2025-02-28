import org.example.dcheck.impl.DocxDocument;
import org.example.dcheck.impl.TextContent;
import org.example.dcheck.impl.fileprocessor.LangChainDocumentProcessor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Date: 2025/2/28
 *
 * @author 三石而立Sunsy
 */
public class TestApp {
    @Test
    public void test() {
        LangChainDocumentProcessor processor = new LangChainDocumentProcessor();
        processor.init();
        processor.split(new DocxDocument("ARM-labguide1.doc", () -> {
            try {
                return Files.newInputStream(Paths.get("docs", "ARM-labguide1.doc"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        })).forEach(doc -> {
            if (doc.getContent() instanceof TextContent) {
                System.out.println(((TextContent) doc.getContent()).getText());
                System.out.println("-------");
            }
        });
    }

}
