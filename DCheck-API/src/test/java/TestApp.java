import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import org.example.dcheck.api.Check;
import org.example.dcheck.api.Content;
import org.example.dcheck.api.Document;
import org.example.dcheck.util.DCheckExecutorService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

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


    @Data
    static class Num {
        private final float v;

        public Num add(Num num) {
            return new Num(v + num.v);
        }
    }

    @Test
    public void testAddNull() {
        List<Integer> ages = Arrays.asList(25, 30, 45, 28, 32);
        System.out.println(ages.stream().reduce(new Num(0), (Num a, Integer b) -> {
            // do accumulate in seperate sub task
            return a.add(new Num(b.floatValue()));
        }, (Num p, Num n) -> {
            // merge sub task in parallel stream
            System.out.println("p: " + p + " n: " + n);
            return p.add(n);
        }));
    }

    @NonNull
    @AllArgsConstructor
    static class Pojo {
        String name;
    }
}
