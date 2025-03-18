package org.example.dcheck.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.function.Supplier;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
 */
@Value
@Builder
@NonFinal
@AllArgsConstructor
@SuppressWarnings("unused")
public class SimpleParagraph {
    @NonNull
    Supplier<Content> content;
    @NonNull
    ParagraphType paragraphType;
    @NonNull
    ParagraphLocation location;

    public Content getContent() {
        return content.get();
    }
}
