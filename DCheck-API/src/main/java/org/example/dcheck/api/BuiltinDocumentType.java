package org.example.dcheck.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public enum BuiltinDocumentType implements DocumentType {
    DOCX,
    PDF,
    UNKNOWN;

    private static final List<BuiltinDocumentType> fastValues = Collections.unmodifiableList(Arrays.asList(values()));

    public static List<? extends DocumentType> fastValues() {
        return fastValues;
    }
}
