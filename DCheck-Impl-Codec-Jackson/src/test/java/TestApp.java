import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.dcheck.Obj;
import org.example.dcheck.impl.codec.jackson.JacksonCodec;
import org.junit.jupiter.api.Test;

/**
 * Date 2025/03/06
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("all")
public class TestApp {

    @Test
    public void test() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.addMixIn(Point.class, PointMixin.class);
        String json = objectMapper.writeValueAsString(new ZPoint(1, 2, 3));
        System.out.println(json);
        Point point = objectMapper.readValue(json, Point.class);
        System.out.println(point);
    }

    @Test
    public void testCodec() throws Exception {
        JacksonCodec codec = new JacksonCodec();
        for (int i = 0; i < 5; i++) {
            String json = codec.serialize(new Obj("a", "b", "c"), String.class);
            System.out.println(json);
            System.out.println((Object) codec.deserialize(json, Obj.class));
        }
    }

    @Data
    @RequiredArgsConstructor
    public static class Point {
        private final int x;
        private final int y;

        public Point(Object others, int x, int y) {
            this.x = x;
            this.y = y;
            System.out.println("others1: " + others);
        }

        @JsonAnySetter
        public void setOthers(int others) {
            System.out.println("others2: " + others);
        }
    }

    public static class PointMixin {
        @JsonCreator
        public PointMixin(Object others, int x, int y) {
        }
    }

    @Getter
    public static class ZPoint extends Point {

        private final int z;

        public ZPoint(int x, int y, int z) {
            super(x, y);
            this.z = z;
        }
    }
}
