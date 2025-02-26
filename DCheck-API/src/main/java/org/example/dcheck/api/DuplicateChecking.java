package org.example.dcheck.api;

/**
 * Date 2025/02/25
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
}
