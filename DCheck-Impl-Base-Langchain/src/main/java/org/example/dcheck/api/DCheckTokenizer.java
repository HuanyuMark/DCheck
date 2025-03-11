package org.example.dcheck.api;

import dev.langchain4j.model.Tokenizer;

/**
 * Date 2025/03/11
 *
 * @author 三石而立Sunsy
 */
public interface DCheckTokenizer extends Tokenizer {
    void init() throws Exception;
}
