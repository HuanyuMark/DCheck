package org.example.dcheck.impl.fileprocessor;

import lombok.Getter;
import lombok.var;
import org.example.dcheck.spi.ConfigProvider;

/**
 * Date: 2025/2/28
 *
 * @author 三石而立Sunsy
 */
public class SharedDocumentProcessorConfig {

    @Getter(lazy = true)
    private static final SharedDocumentProcessorConfig instance = new SharedDocumentProcessorConfig() {{
        init();
    }};

    public static final String MAX_PARAGRAPH_LENGTH = "file-processor.default.docx.max-paragraph-length";

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
            var apiConfig = ConfigProvider.getInstance().getApiConfig();
            //TODO read config init
            try {
                maxParagraphLength = Integer.parseInt(apiConfig.getProperty(MAX_PARAGRAPH_LENGTH));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid config '" + MAX_PARAGRAPH_LENGTH + "=" + apiConfig.getProperty(MAX_PARAGRAPH_LENGTH) + "'", e);
            }
            init = true;
        }
    }
}
