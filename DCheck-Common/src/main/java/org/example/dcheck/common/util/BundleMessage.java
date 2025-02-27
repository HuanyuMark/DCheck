package org.example.dcheck.common.util;

import lombok.Data;
import lombok.Getter;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Date: 2025/2/27
 *
 * @author 三石而立Sunsy
 */
@Data
public class BundleMessage {
    private final String bundle;
    private final String key;
    private final Locale locale;

    @Getter(lazy = true)
    private final String message = ResourceBundle.getBundle(bundle, locale).getString(key);

    public String format(Map<String, Object> args) {
        return MessageFormat.format(getMessage(), args);
    }
}
