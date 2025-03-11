package org.example.dcheck.api.embedding;

import org.springframework.lang.Nullable;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface EmbeddingFunction {

    /**
     * 请在init()后在调用。实现类
     * 返回一个合规的类名以标识该实现类，该类名可以不存在
     */
    default String getName() {
        try {
            init();
        } catch (Exception e) {
            throw new IllegalStateException("init EmbeddingFunction '" + getClass().getName() + "' fail: " + e.getMessage(), e);
        }
        return getClass().getName();
    }

    /**
     * 请在init()后在调用。
     * 返回描述这个embedding function的详细信息
     */
    @Nullable
    default Map<String, Object> getDetails() {
        return null;
    }


    void init() throws Exception;

    Embedding embedQuery(String query) throws Exception;

    List<Embedding> embedDocuments(List<String> documents) throws Exception;

    List<Embedding> embedDocuments(String[] documents) throws Exception;

    //TODO 支持多模态嵌入
    default List<Embedding> embedUnknownTypeDocuments(List<Supplier<InputStream>> documents) throws Exception {
        throw new Exception();
    }
}
