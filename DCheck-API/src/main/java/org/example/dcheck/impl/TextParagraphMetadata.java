package org.example.dcheck.impl;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.example.dcheck.api.BuiltinParagraphType;
import org.example.dcheck.api.ParagraphLocation;
import org.example.dcheck.api.ParagraphMetadata;
import org.example.dcheck.api.ParagraphType;

import java.util.HashMap;
import java.util.Map;

/**
 * Date 2025/02/27
 *
 * @author 三石而立Sunsy
 */
@Getter
@Builder
@ToString
@SuppressWarnings("unused")
@EqualsAndHashCode(callSuper = true)
public class TextParagraphMetadata extends HashMap<String, Object> implements ParagraphMetadata {
    private final String documentId;
    private final ParagraphLocation location;

    public TextParagraphMetadata(String documentId, ParagraphLocation location) {
        this.documentId = documentId;
        this.location = location;
        put("documentId", documentId);
        put("location", location);
    }

    public TextParagraphMetadata(Map<? extends String, ?> m, String documentId, ParagraphLocation location) {
        super(m);
        this.documentId = documentId;
        this.location = location;
        put("documentId", documentId);
        put("location", location);
    }

    @Override
    public ParagraphType getParagraphType() {
        return BuiltinParagraphType.TEXT;
    }
}
