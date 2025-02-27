package org.example.dcheck.api;

import lombok.var;

import java.util.List;

/**
 * Date 2025/02/25
 * the endpoint to start duplicate-check
 * 进行查重的入口
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface DuplicateChecking {

    /**
     * 初始化. 同步方法，会准备所有需要的资源，包括完成从网络下载资源，解压等工作。可以在正式
     * 调用其他api前调用该方法已提前初始化以加快第一次调用其他api的速度
     */
    void init();

    /**
     * 获取相似度引擎，以获取 {@link DocumentCollection}
     */
    ParagraphRelevancyEngine getRelevancyEngine();

    /**
     * 根据配置的检查，在指定集合里查重
     */
    CheckResult check(Check check, DocumentCollection collection);

    /**
     * 根据配置的检查，在指定集合里查重.
     * Note: 该方法只适合零时使用，如果这些Document需要被持久化或者在其他地方复用，
     * 请按照以下方式调用 1. {@link #getRelevancyEngine()} 2. {@link ParagraphRelevancyEngine#getOrCreateDocumentCollection(String)}
     * 3. {@link #check(Check, DocumentCollection)}
     */
    default CheckResult check(Check check, List<Document> tempCheckArea) {
        try (var collection = getRelevancyEngine().newTempDocumentCollection()) {
            collection.addDocument(tempCheckArea);
            return check(check, collection);
        }
    }
}
