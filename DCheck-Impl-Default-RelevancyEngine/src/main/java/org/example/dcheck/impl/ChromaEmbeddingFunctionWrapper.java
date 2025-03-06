package org.example.dcheck.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.cglib.core.DefaultNamingPolicy;
import org.springframework.cglib.core.Predicate;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.NoOp;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Date: 2025/3/3
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
@RequiredArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ChromaEmbeddingFunctionWrapper implements EmbeddingFunction {

    @RequiredArgsConstructor
    protected static class WrapperProxyNamingPolicy extends DefaultNamingPolicy {
        //@see DefaultNamingPolicy.STRESS_HASH_CODE
        protected static final boolean STRESS_HASH_CODE = Boolean.getBoolean("org.springframework.cglib.test.stressHashCodes");

        protected final org.example.dcheck.api.embedding.EmbeddingFunction target;

        @Override
        public String getClassName(String prefix, String source, Object key, Predicate names) {
            int index = 0;
            String attempt = target.getName() + "$Wrapper_" + index++;
            while (names.evaluate(attempt)) {
                attempt = target.getName() + "$Wrapper_" + index++;
            }
            return attempt;
        }
    }

    protected static class Wrapper extends Enhancer {
        public Wrapper(org.example.dcheck.api.embedding.EmbeddingFunction target) {
            setNamingPolicy(new WrapperProxyNamingPolicy(target));
            setCallback(NoOp.INSTANCE);
            setAttemptLoad(true);
            setSuperclass(ChromaEmbeddingFunctionWrapper.class);
        }
    }

    public static EmbeddingFunction wrap(@NonNull org.example.dcheck.api.embedding.EmbeddingFunction target) {
        return (EmbeddingFunction) new Wrapper(target).create(new Class[]{org.example.dcheck.api.embedding.EmbeddingFunction.class}, new Object[]{target});
    }


    protected final org.example.dcheck.api.embedding.EmbeddingFunction target;

    @Override
    public Embedding embedQuery(String query) throws EFException {
        try {
            return ChromaWrappedEmbedding.wrap(target.embedQuery(query));
        } catch (Throwable e) {
            throw wrapException(e);
        }
    }

    protected EFException wrapException(Throwable e) {
        return e instanceof EFException ? (EFException) e : new EFException(e);
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws EFException {
        try {
            return target.embedDocuments(documents).stream().map(ChromaWrappedEmbedding::wrap).collect(Collectors.toList());
        } catch (Throwable e) {
            throw wrapException(e);
        }
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws EFException {
        try {
            return target.embedDocuments(documents).stream().map(ChromaWrappedEmbedding::wrap).collect(Collectors.toList());
        } catch (Throwable e) {
            throw wrapException(e);
        }
    }
}
