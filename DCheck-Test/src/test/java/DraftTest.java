import lombok.Data;
import org.example.dcheck.impl.ContentMatchParagraphLocation;
import org.example.dcheck.impl.TextParagraphLocation;
import org.example.dcheck.impl.TextParagraphMetadata;
import org.example.dcheck.impl.codec.jackson.JacksonCodec;
import org.junit.jupiter.api.Test;

/**
 * Date 2025/03/13
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("all")
public class DraftTest {
    @Test
    public void testJackson() throws Exception {
        JacksonCodec codec = new JacksonCodec();
//        String json = codec.serialize(new Obj("a", "b", "c"), String.class);
        String json = "{\"startText\":\"# 城市公园景观提升项目招标文\",\"endText\":\"\\n在功能优化上，我们将合理规划不同区域功能。增加休闲座椅、健身设施、儿童游乐区等，满足不同年龄段人群需求。例如，在阳光充足、视野开阔区域设置健身广场，配备多样化健身器材；在相对安静、绿树环绕处打造儿童游乐区，设置安全有趣的游乐设施，让孩子在自然环境中快乐玩耍。\",\"splitIdx\":0,\"type\":\"CONTENT_MATCH\"}";
        System.out.println(json);
        System.out.println((Object) codec.deserialize(json, ContentMatchParagraphLocation.class));
    }

    @Test
    public void testMetadataSync() throws Exception {
        TextParagraphMetadata metadata = new TextParagraphMetadata("asd", new TextParagraphLocation(1));
        System.out.println(metadata.size());
        System.out.println(metadata);
    }


    @Data
    static class TestRecord {
        private final String name;
    }
}
