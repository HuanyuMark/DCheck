package org.example.dcheck.api;

import lombok.*;
import lombok.experimental.NonFinal;
import org.jetbrains.annotations.Range;

import java.util.*;

/**
 * Date: 2025/2/25
 * represent a check task.
 * core api to start a duplicate checking
 *
 * @author 三石而立Sunsy
 */
@Value
@Builder
@NonFinal
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Check {

    /**
     * 查重文件
     */
    @NonNull
    Document document;

    /**
     * 文件会被划分为若干个段落，每个段落都会单独查找在同一个文档集合中（存放着所有file的集合）语义最相似的段落，
     * topKOfEachParagraph表示查找每个段落中前topK个语义最相似的段落数量
     * @see CheckResult#getDuplicateParts()
     * @see DuplicatePart#getDuplicates() DuplicatePart#getDuplicates().size()
     */
    @Builder.Default
    @Range(from = 1, to = Long.MAX_VALUE)
    int topKOfEachParagraph = 5;

    /**
     * 最后所有相关段落所在的文档都会整体进行相似度计算，topKOfDocument表示最后计算前topK个最相似的文档
     * @see CheckResult#getRelevantDocuments() CheckResult#getRelevantDocuments().size()
     */
    @Builder.Default
    @Range(from = 1, to = Long.MAX_VALUE)
    int topKOfDocument = 5;

    /**
     * 如果相关性分数小于minParagraphRelevancy，则该段落会被忽略，不会参与后续的相似度计算
     * @see DuplicatePart.DuplicateParagraph#getRelevancy()
     */
    @Range(from = 0, to = Long.MAX_VALUE)
    double minParagraphRelevancy;

    /**
     * 如果相关性分数小于minDocumentRelevancy，则该文档会被忽略，不会参与后续的相似度计算
     * @see CheckResult.RelevantDocument#getScore()
     */
    @Range(from = 0, to = Long.MAX_VALUE)
    double minDocumentRelevancy;

    /**
     * these white list will be used to filter the duplicate parts
     */
    @Singular
    List<@NonNull WhiteListRuleSet> whiteLists;

    /**
     * 白名单过滤阈值. 越接近1则表示无视越多的 WhiteListRule 所认为的“不重复”片段，及控制whiteList的筛选力度，
     * 该值越高则代表whiteList筛选力度越低。为0则代表全盘采用 WhiteListRule 的筛选结果
     * [0,1]
     */
    @Builder.Default
    @Range(from = 0, to = 1)
    double whiteListThreshold = 0.5;

    public static CheckBuilder builder() {
        return new ValidatedCheckBuilder();
    }

    protected static class ValidatedCheckBuilder extends CheckBuilder {

        @Override
        public Check build() {
            Check target = super.build();
            if (target.getTopKOfEachParagraph() < 1) {
                throw new IllegalArgumentException("topKOfEachParagraph must be >= 1");
            }
            if (target.getTopKOfDocument() < 1) {
                throw new IllegalArgumentException("topKOfDocument must be >= 1");
            }
            if (target.getMinParagraphRelevancy() < 0) {
                throw new IllegalArgumentException("minParagraphRelevancy must be >= 0");
            }
            if (target.getMinDocumentRelevancy() < 0) {
                throw new IllegalArgumentException("minDocumentRelevancy must be >= 0");
            }
            if (target.getWhiteListThreshold() < 0 || target.getWhiteListThreshold() > 1) {
                throw new IllegalArgumentException("whiteListThreshold must be >= 0 and <= 1");
            }

            Set<WhiteListRuleSet> sets = new LinkedHashSet<>(target.getWhiteLists());
            if (sets.size() == target.getWhiteLists().size()) {
                return target;
            }

            return new Check(
                    target.getDocument(),
                    target.getTopKOfEachParagraph(),
                    target.getTopKOfDocument(),
                    target.getMinParagraphRelevancy(),
                    target.getMinDocumentRelevancy(),
                    Collections.unmodifiableList(new ArrayList<>(sets)),
                    target.getWhiteListThreshold()
            );
        }
    }
}
