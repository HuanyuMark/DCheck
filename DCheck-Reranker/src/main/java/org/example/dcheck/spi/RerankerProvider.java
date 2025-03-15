package org.example.dcheck.spi;

import lombok.Getter;
import org.example.dcheck.api.Reranker;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Date: 2025/2/27
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public class RerankerProvider implements DCheckProvider {
    @Getter(lazy = true)
    private static final RerankerProvider instance = new RerankerProvider();

    @Nullable
    @Getter(lazy = true)
    private final Reranker reranker = Providers.findImpl(Reranker.class, "dcheck.reranker.impl");

    public List<Reranker> getRerankers() {
        return Providers.findAllImplementations(Reranker.class);
    }
}
