package org.example.dcheck.api;

import lombok.Data;
import org.example.dcheck.spi.DuplicateCheckingProvider;

/**
 * Date 2025/03/11
 *
 * @author 三石而立Sunsy
 */
@Data
public class FileProcessorTokenizerInjectionEvent {
    private final DCheckTokenizer tokenizer;

    public static void publish(Object tokenizer) {
        if (!(tokenizer instanceof DCheckTokenizer)) {
            throw new IllegalArgumentException("tokenizer must be DCheckTokenizer");
        }
        DuplicateCheckingProvider.getInstance().getChecking().emitEvent(new FileProcessorTokenizerInjectionEvent((DCheckTokenizer) tokenizer)).join();
    }
}
