package org.example.dcheck.spi;

import lombok.Getter;
import lombok.var;
import org.example.dcheck.api.Reranker;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Date: 2025/2/27
 *
 * @author 三石而立Sunsy
 */
public class RerankerProvider implements DCheckProvider {
    @Getter(lazy = true)
    private static final RerankerProvider instance = new RerankerProvider();

    @Nullable
    @Getter(lazy = true)
    private final Reranker reranker = loadReranker();

    public List<Reranker> getRerankers() {
        return Providers.findAllImplementations(Reranker.class);
    }

    private Reranker loadReranker() {
        var rerankers = getRerankers();
        if (rerankers.isEmpty()) return null;
        if (rerankers.size() == 1) {
            return rerankers.get(0);
        }
        var specifyClass = System.getProperty("dcheck.reranker.impl");
        if (specifyClass == null) {
            throw new IllegalStateException("multiple reranker impl found: please add single implementation on classpath or" +
                    " specify implementation with jvm arg '-Ddcheck.reranker.impl=<impl canonical name>', find implementations: " + rerankers);
        }
        for (var reranker : rerankers) {
            if (specifyClass.equals(reranker.getClass().getCanonicalName())) {
                return reranker;
            }
        }
        throw new IllegalStateException("no reranker provider '" + specifyClass + "' found: please add implementation service provider on classpath");
    }
}
