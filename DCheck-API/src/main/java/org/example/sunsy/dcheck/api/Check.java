package org.example.sunsy.dcheck.api;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
 */
@Data
@Builder
public class Check {
    /**
     * 查重文件
     */
    @NonNull
    private final Document document;
    /**
     * 文件会被划分为若干个段落，每个段落都会单独查找在同一个文档集合中（存放着所有file的集合）语义最相似的段落，
     * topKOfEachParagraph表示查找每个段落中前topK个语义最相似的段落数量
     */
    @Builder.Default
    private final int topKOfEachParagraph = 5;
    /**
     * 最后所有相关段落所在的文档都会整体进行相似度计算，topKOfDocument表示最后计算前topK个最相似的文档
     */
    @Builder.Default
    private final int topKOfDocument = 5;
}
