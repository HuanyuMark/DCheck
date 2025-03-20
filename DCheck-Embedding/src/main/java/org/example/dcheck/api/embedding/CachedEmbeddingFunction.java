package org.example.dcheck.api.embedding;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.example.dcheck.api.Content;
import org.example.dcheck.api.DCheckConfig;
import org.example.dcheck.spi.DCheckConfigProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Date: 2025/3/19
 * cache the embedding result of the text
 *
 * @author 三石而立Sunsy
 */
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@SuppressWarnings("unused")
public class CachedEmbeddingFunction implements EmbeddingFunction {

    @Getter
    @EqualsAndHashCode.Include
    private final EmbeddingFunction target;

    @ToString.Exclude
    private LoadingCache<String, Embedding> textEmbeddingCache;

    @Getter
    private int cacheSize = -1;
    @Getter
    private long expireTime = -1;

    protected CachedEmbeddingFunction(@NonNull EmbeddingFunction target) {
        this.target = target;
    }

    public void setCacheSize(int cacheSize) {
        if (cacheSize <= 0) {
            throw new IllegalArgumentException("cacheSize must be > 0");
        }
        this.cacheSize = cacheSize;
        if (textEmbeddingCache != null) {
            textEmbeddingCache.policy().eviction().ifPresent(ev -> ev.setMaximum(getCacheSize()));
        }
    }

    public void setExpireTime(long expireTime) {
        if (expireTime <= 0) {
            throw new IllegalArgumentException("expireTime must be > 0");
        }
        this.expireTime = expireTime;
        if (textEmbeddingCache != null) {
            textEmbeddingCache.policy().expireAfterAccess().ifPresent(ev -> ev.setExpiresAfter(getExpireTime(), TimeUnit.MICROSECONDS));
        }
    }

    public static CachedEmbeddingFunction wrap(@NonNull EmbeddingFunction target) {
        if (target instanceof CachedEmbeddingFunction) return ((CachedEmbeddingFunction) target);
        return new CachedEmbeddingFunction(target);
    }

    @Override
    public void init() throws Exception {
        getTarget().init();

        DCheckConfig config = DCheckConfigProvider.getInstance().getDCheckConfig();

        textEmbeddingCache = Caffeine.newBuilder()
                .build(new CacheLoader<String, Embedding>() {
                    @Override
                    public Embedding load(@NotNull String key) throws Exception {
                        return getTarget().embedQuery(key);
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public @NotNull Map<String, Embedding> loadAll(@NotNull Iterable<? extends @NotNull String> keys) throws Exception {
                        List<String> requests;
                        if (keys instanceof List<?>) {
                            requests = (List<String>) keys;
                        } else {
                            requests = StreamSupport.stream(keys.spliterator(), false).collect(Collectors.toList());
                        }
                        List<Embedding> result = getTarget().embedDocuments(requests);
                        Map<String, Embedding> hit = new HashMap<>((int) Math.ceil(requests.size() / 0.75f));
                        int size = requests.size();
                        for (int i = 0; i < size; i++) {
                            hit.put(requests.get(i), result.get(i));
                        }
                        return hit;
                    }
                });

        if (cacheSize < 0) {
            setCacheSize(config.requiredPositiveInt(EmbeddingApiConfigKey.EMBEDDING_CACHE_SIZE));
        } else {
            setCacheSize(getCacheSize());
        }

        if (expireTime < 0) {
            setExpireTime(config.requiredPositiveInt(EmbeddingApiConfigKey.EMBEDDING_CACHE_EXPIRE_TIME));
        } else {
            setExpireTime(getExpireTime());
        }
    }

    @Override
    public void inited() throws Exception {
        getTarget().inited();
    }

    @Override
    public void close() throws Exception {
        getTarget().close();
        textEmbeddingCache.cleanUp();
        textEmbeddingCache = null;
    }

    @Override
    public String getName() {
        return getTarget().getName();
    }

    @Override
    public @Nullable Map<String, Object> getDetails() {
        return getTarget().getDetails();
    }

    @Override
    public Embedding embedQuery(String query) throws Exception {
        return textEmbeddingCache.get(query);
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws Exception {
        Map<String, Embedding> all = textEmbeddingCache.getAll(documents);
        return documents.stream().map(all::get).collect(Collectors.toList());
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws Exception {
        return embedDocuments(Arrays.asList(documents));
    }

    @Override
    public List<Embedding> embedUnknownTypeDocuments(List<Content> documents) throws Exception {
        return getTarget().embedUnknownTypeDocuments(documents);
    }
}
