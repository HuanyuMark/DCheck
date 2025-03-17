package org.example.dcheck.impl;

import lombok.Getter;
import org.example.dcheck.api.DCheckConfig;
import org.example.dcheck.spi.DCheckConfigProvider;

/**
 * Date: 2025/2/28
 *
 * @author 三石而立Sunsy
 */
public class SharedDocumentProcessorConfig {

    public static final String MAX_PARAGRAPH_LENGTH = "file-processor.split.text.max-paragraph-length";
    @Getter(lazy = true)
    private static final SharedDocumentProcessorConfig instance = new SharedDocumentProcessorConfig() {{
        init();
    }};
    @Getter
    private int maxParagraphLength;


    private volatile boolean init;

    public void init() {
        if (init) {
            return;
        }
        synchronized (this) {
            if (init) {
                return;
            }
            DCheckConfig DCheckConfig = DCheckConfigProvider.getInstance().getDCheckConfig();
            //TODO read config init
            maxParagraphLength = DCheckConfig.requiredPositiveInt(MAX_PARAGRAPH_LENGTH);
            init = true;
        }
    }
}
