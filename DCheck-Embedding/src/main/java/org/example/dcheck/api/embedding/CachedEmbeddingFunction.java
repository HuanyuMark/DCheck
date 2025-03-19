package org.example.dcheck.api.embedding;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.dcheck.api.Content;
import org.example.dcheck.spi.DCheckConfigProvider;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.ConcurrentLruCache;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Date: 2025/3/19
 * cache the embedding result of the text
 *
 * @author 三石而立Sunsy
 */
@Data
@Getter
@RequiredArgsConstructor
public class CachedEmbeddingFunction implements EmbeddingFunction {

    @NonNull
    private final EmbeddingFunction target;

    private final ThreadLocal<@Nullable Map<String, Embedding>> batchTextEmbeddingCache = new ThreadLocal<>();

    private ConcurrentLruCache<String, Embedding> textEmbeddingCache;

    @Override
    public void init() throws Exception {
        getTarget().init();

        Integer cacheSize = DCheckConfigProvider.getInstance().getDCheckConfig().requiredPositiveInt(EmbeddingApiConfigKey.EMBEDDING_CACHE_SIZE);

        textEmbeddingCache = new ConcurrentLruCache<>(cacheSize, key -> {
            Map<String, Embedding> area = batchTextEmbeddingCache.get();
            if (area != null) {
                Embedding embedding = area.get(key);
                if (embedding != null) return embedding;
            }
            try {
                return getTarget().embedQuery(key);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void inited() throws Exception {
        getTarget().inited();
    }

    @Override
    public void close() throws Exception {
        getTarget().close();
        textEmbeddingCache.clear();
        textEmbeddingCache = new ConcurrentLruCache<>(1, s -> Embedding.from(new float[]{}));
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

        List<String> missedDocs = documents.stream().filter(s -> !textEmbeddingCache.contains(s)).collect(Collectors.toList());

        if (missedDocs.isEmpty()) {
            return documents.stream().map(doc -> textEmbeddingCache.get(doc)).collect(Collectors.toList());
        }

        List<Embedding> missedEmbeddings = getTarget().embedDocuments(missedDocs);


        Map<String, Embedding> preTextEmbeddingCacheValue = IntStream.range(0, missedDocs.size()).mapToObj(i -> new AbstractMap.SimpleEntry<>(missedDocs.get(i), missedEmbeddings.get(i))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        // avoid memory leak
        batchTextEmbeddingCache.remove();
        batchTextEmbeddingCache.set(preTextEmbeddingCacheValue);
        try {
            for (String toStore : missedDocs) {
                // emit cache operation: store in the cache
                embedQuery(toStore);
            }
        } finally {
            batchTextEmbeddingCache.remove();
        }

        // use local construction to avoid the cache miss and concurrent race
        return documents.stream().map(doc -> {
            Embedding embedding = preTextEmbeddingCacheValue.get(doc);
            if (embedding != null) return embedding;
            return textEmbeddingCache.get(doc);
        }).collect(Collectors.toList());
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
