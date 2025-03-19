import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.example.dcheck.api.Check;
import org.example.dcheck.api.Content;
import org.example.dcheck.api.Document;
import org.example.dcheck.util.DCheckExecutorService;
import org.junit.jupiter.api.Test;

/**
 * Date 2025/02/25
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("all")
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

    @Test
    public void testScript() throws Exception {
        DCheckExecutorService exe = new DCheckExecutorService() {
            {
                init();
            }
        };

        for (int i = 0; i < 10; i++) {
//            System.out.println(exe.getThreadFactory().newThread(() -> {
//            }));
            exe.submit(() -> {
                System.out.println(Thread.currentThread());
            });
        }

//        ScriptEngineManager engineManager = new ScriptEngineManager(getClass().getClassLoader());
//        ScriptEngine engine = engineManager.getEngineByExtension("groovy");
//        System.out.println(engine.eval("Thread.ofVirtual().name(\"v-\").factory()"));
    }

    @Test
    public void testAddNull() {
        new Pojo(null);
    }

    @NonNull
    @AllArgsConstructor
    static class Pojo {
        String name;
    }
}
