package org.example.dcheck.api;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

/**
 * Date: 2025/3/11
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface DCheckDocumentSplitter extends DCheckComponent, DocumentSplitter {

    static DCheckDocumentSplitter wrap(DocumentSplitter raw) {
        if (raw instanceof DCheckDocumentSplitter) return (DCheckDocumentSplitter) raw;
        return new DCheckDocumentSplitter() {
            @Override
            public List<TextSegment> split(Document document) {
                return raw.split(document);
            }

            @Override
            public List<TextSegment> splitAll(List<Document> documents) {
                return raw.splitAll(documents);
            }
        };
    }
}
