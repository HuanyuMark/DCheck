import org.example.dcheck.impl.DocxDocument;
import org.example.dcheck.impl.InMemoryTextContent;
import org.example.dcheck.impl.PdfDocument;
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
    public void testDoc() {
        LangChainDocumentProcessor processor = new LangChainDocumentProcessor();
        processor.init();
        processor.split(new DocxDocument("ARM-labguide1.doc", () -> {
            try {
                return Files.newInputStream(Paths.get("docs", "ARM-labguide1.doc"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        })).forEach(doc -> {
            if (doc.getContent() instanceof InMemoryTextContent) {
                System.out.println(((InMemoryTextContent) doc.getContent()).getText());
                System.out.println("-------");
            }
        });
    }

    @Test
    public void testPdf() {
        LangChainDocumentProcessor processor = new LangChainDocumentProcessor();
        processor.init();
        processor.split(new PdfDocument("ARM-labguide1.pdf", () -> {
            try {
                return Files.newInputStream(Paths.get("docs", "ARM-labguide1.pdf"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        })).forEach(doc -> {
            if (doc.getContent() instanceof InMemoryTextContent) {
                System.out.println(((InMemoryTextContent) doc.getContent()).getText());
                System.out.println("-------");
            }
        });
    }
}
