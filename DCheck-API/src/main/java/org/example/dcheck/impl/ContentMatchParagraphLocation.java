package org.example.dcheck.impl;

import lombok.Data;
import lombok.NonNull;
import org.example.dcheck.api.BuiltinParagraphLocationType;
import org.example.dcheck.api.ParagraphLocation;
import org.example.dcheck.api.ParagraphLocationType;

/**
 * Date: 2025/2/28
 * 需要在运行时根据Paragraph内容进行匹配计算出的位置（仅用在方便匹配且在处理Document时较难获取对应Paragraph位置时使用）
 *
 * @author 三石而立Sunsy
 */
@Data
public class ContentMatchParagraphLocation implements ParagraphLocation {
    @NonNull
    private final String startText;
    @NonNull
    private final String endText;

    private final int splitIdx;

    public static ContentMatchParagraphLocation formLine(String text, int splitIdx) {
        String trim = text.trim();
        String startText;
        String endText;
        int firstIdx = trim.indexOf("\n");
        if (firstIdx < 0) {
            startText = trim;
        } else {
            startText = trim.substring(0, firstIdx);
        }
        int lastIdx = trim.lastIndexOf("\n");
        if (lastIdx < 0) {
            endText = trim;
        } else {
            endText = trim.substring(lastIdx);
        }
        return new ContentMatchParagraphLocation(startText, endText, splitIdx);
    }

    public ParagraphLocationType getType() {
        return BuiltinParagraphLocationType.CONTENT_MATCH;
    }
}
