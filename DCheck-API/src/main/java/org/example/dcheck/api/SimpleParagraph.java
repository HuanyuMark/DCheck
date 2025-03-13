package org.example.dcheck.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.function.Supplier;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
 */
@Data
@Builder
@AllArgsConstructor
@SuppressWarnings("unused")
public class SimpleParagraph {
    @NonNull
    private final Supplier<Content> content;
    @NonNull
    private final ParagraphType paragraphType;
    @NonNull
    private final ParagraphLocation location;

    public Content getContent() {
        return content.get();
    }
}
