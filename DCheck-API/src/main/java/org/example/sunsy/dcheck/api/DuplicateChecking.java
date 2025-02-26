package org.example.sunsy.dcheck.api;

/**
 * Date 2025/02/25
 * 进行查重的入口
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface DuplicateChecking {
    /**
     * 获取相似度引擎，以获取 {@link DocumentCollection}
     */
    ParagraphRelevancyEngine getRelevancyEngine();

    /**
     * 根据配置的检查，在指定集合里查重
     */
    CheckResult check(Check check, DocumentCollection collection);
}
