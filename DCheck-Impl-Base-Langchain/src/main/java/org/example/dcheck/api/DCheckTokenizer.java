package org.example.dcheck.api;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.Tokenizer;

/**
 * Date 2025/03/11
 *
 * @author 三石而立Sunsy
 */
public interface DCheckTokenizer extends Tokenizer, DCheckComponent {

    DCheckTokenizer NONE = new DCheckTokenizer() {
        private final String name = DCheckTokenizer.class.getSimpleName() + ".NONE";

        @Override
        public int estimateTokenCountInText(String text) {
            return 0;
        }

        @Override
        public int estimateTokenCountInMessage(ChatMessage message) {
            return 0;
        }

        @Override
        public int estimateTokenCountInMessages(Iterable<ChatMessage> messages) {
            return 0;
        }

        @Override
        public int estimateTokenCountInToolSpecifications(Iterable<ToolSpecification> toolSpecifications) {
            return 0;
        }

        @Override
        public int estimateTokenCountInToolExecutionRequests(Iterable<ToolExecutionRequest> toolExecutionRequests) {
            return 0;
        }

        @Override
        public void init() {
        }

        @Override
        public String toString() {
            return name;
        }
    };
}
