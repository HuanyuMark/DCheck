package org.example.dcheck.common.util;

import java.util.Locale;

/**
 * Date: 2025/2/27
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public class I18nMessage extends BundleMessage {
    public I18nMessage(String bundle, String key, Locale locale) {
        super("/i18n/" + bundle + ".properties", key, locale);
    }
}
