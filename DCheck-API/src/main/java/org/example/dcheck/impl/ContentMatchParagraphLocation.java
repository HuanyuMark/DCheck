package org.example.dcheck.impl;

import org.example.dcheck.api.BuiltinParagraphLocationType;
import org.example.dcheck.api.ParagraphLocation;
import org.example.dcheck.api.ParagraphLocationType;

/**
 * Date: 2025/2/28
 * 需要在运行时根据Paragraph内容进行匹配计算出的位置（仅用在方便匹配且在处理Document时较难获取对应Paragraph位置时使用）
 *
 * @author 三石而立Sunsy
 */
public class ContentMatchParagraphLocation implements ParagraphLocation {
    private static final ContentMatchParagraphLocation ins = new ContentMatchParagraphLocation();

    public static ContentMatchParagraphLocation get() {
        return ins;
    }

    @Override
    public ParagraphLocationType getType() {
        return BuiltinParagraphLocationType.CONTENT_MATCH;
    }
}
