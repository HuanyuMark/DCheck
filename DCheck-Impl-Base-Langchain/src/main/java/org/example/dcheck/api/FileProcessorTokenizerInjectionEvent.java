package org.example.dcheck.api;

import lombok.Data;

/**
 * Date 2025/03/11
 *
 * @author 三石而立Sunsy
 */
@Data
public class FileProcessorTokenizerInjectionEvent {
    private final DCheckTokenizer tokenizer;

    public static void publish(IEventEmitter emitter, Object tokenizer) {
        if (!(tokenizer instanceof DCheckTokenizer)) {
            throw new IllegalArgumentException("tokenizer must be DCheckTokenizer");
        }
        emitter.emitEvent(new FileProcessorTokenizerInjectionEvent((DCheckTokenizer) tokenizer)).join();
    }
}
