package org.example.dcheck.common.util;

import lombok.var;

import java.util.Map;
import java.util.Objects;

/**
 * Date: 2025/2/27
 *
 * @author 三石而立Sunsy
 */
public class MessageFormat {
    /**
     * 使用给定的参数替换模板中的占位符
     *
     * @param template 原始模板字符串，包含待替换的占位符 {@code {argName}}
     * @param args     包含占位符和对应替换值的映射
     * @return 替换后的字符串
     */
    public static String format(String template, Map<String, Object> args) {
        var builder = new StringBuilder(template);
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            var key = "{" + entry.getKey() + "}";
            var value = Objects.toString(entry.getValue());
            int idx;
            int startIdx = 0;
            while ((idx = builder.indexOf(key, startIdx)) >= 0) {
                builder.replace(idx, idx + key.length(), value);
                startIdx = idx + value.length();
            }
        }
        return builder.toString();
    }

    public static String format(String template, Object... args) {
        return java.text.MessageFormat.format(template, args);
    }
}
