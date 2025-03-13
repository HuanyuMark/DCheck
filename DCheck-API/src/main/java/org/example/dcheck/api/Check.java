package org.example.dcheck.api;

import lombok.*;

/**
 * Date: 2025/2/25
 * represent a check task.
 * core api to start a duplicate checking
 *
 * @author 三石而立Sunsy
 */
@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
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

    /**
     * 如果相关性分数小于minParagraphRelevancy，则该段落会被忽略，不会参与后续的相似度计算
     */
    private final double minParagraphRelevancy;

    /**
     * 如果相关性分数小于minDocumentRelevancy，则该文档会被忽略，不会参与后续的相似度计算
     */
    private final double minDocumentRelevancy;

    @SuppressWarnings("unused")
    public static class CheckBuilder {
        public Check build() {
            if (topKOfEachParagraph$value < 1) {
                throw new IllegalArgumentException("topKOfEachParagraph must be >= 1");
            }
            if (topKOfDocument$value < 1) {
                throw new IllegalArgumentException("topKOfDocument must be >= 1");
            }
            if (minParagraphRelevancy < 0) {
                throw new IllegalArgumentException("minParagraphRelevancy must be >= 0");
            }
            if (minDocumentRelevancy < 0) {
                throw new IllegalArgumentException("minDocumentRelevancy must be >= 0");
            }
            return new Check(document, topKOfEachParagraph$value, topKOfDocument$value, minParagraphRelevancy, minDocumentRelevancy);
        }
    }
}
