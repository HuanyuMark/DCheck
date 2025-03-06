package org.example.dcheck.impl.fileprocessor;

import dev.langchain4j.data.document.DocumentSource;
import dev.langchain4j.data.document.Metadata;
import lombok.Data;
import org.example.dcheck.api.Document;

import java.io.InputStream;

/**
 * Date: 2025/2/27
 *
 * @author 三石而立Sunsy
 */
@Data
public class DCheckDocumentSource implements DocumentSource {

    private final Document document;

    @Override
    public InputStream inputStream() {
        return document.getContent().getInputStream();
    }

    @Override
    public Metadata metadata() {
        return new Metadata().put("id", document.getId())
                .put("documentType", document.getDocumentType().name());
    }
}
