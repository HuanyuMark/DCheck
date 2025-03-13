import org.example.dcheck.api.Check;
import org.example.dcheck.api.Content;
import org.example.dcheck.api.Document;
import org.junit.jupiter.api.Test;

/**
 * Date 2025/02/25
 *
 * @author 三石而立Sunsy
 */
public class TestApp {
    @Test
    public void test() {
        Check.builder().document(new Document() {
            @Override
            public String getId() {
                return null;
            }

            @Override
            public Content getContent() {
                return null;
            }
        }).minParagraphRelevancy(1).build();
        System.out.println("ok");
//        System.out.println(ParagraphRelevancyQueryResult.Record.builder().document("ok?").build().getMetadata());
    }
}
