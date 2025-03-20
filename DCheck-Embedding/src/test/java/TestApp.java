import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Date 2025/03/20
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("all")
public class TestApp {

    @Test
    public void testCaffeine() {
        LoadingCache<String, String> cache = Caffeine.newBuilder()
                .build(new CacheLoader<String, String>() {
                    @Override
                    public String load(@NonNull String key) throws Exception {
                        System.out.println("load: " + key);
                        try {
                            return key;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public @NonNull Map<@NonNull String, @NonNull String> loadAll(@NonNull Iterable<? extends @NonNull String> keys) throws Exception {
                        System.out.println("loadAll: " + keys);
                        Map<String, String> res = new HashMap<>();
                        for (String key : keys) {
                            res.put(key, key);
                        }
                        return res;
                    }
                });

        cache.get("k1");

        cache.getAll(Arrays.asList("k1", "k2"));

    }

}
