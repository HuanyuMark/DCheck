package org.example.sunsy.dcheck.api;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
 */
@Data
@Builder
@SuppressWarnings("unused")
public class DocumentParagraph {
    @NonNull
    private final Supplier<InputStream> content;
    @NonNull
    private final ParagraphType paragraphType;

    public InputStream getContent() {
        return content.get();
    }
}
