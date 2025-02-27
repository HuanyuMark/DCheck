package org.example.dcheck.impl.fileprocessor;

import lombok.Data;
import lombok.var;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.example.dcheck.api.*;
import org.example.dcheck.spi.ConfigProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * Date 2025/02/27
 *
 * @author 三石而立Sunsy
 */
@Data
public class PoiDocxDocumentProcessor implements DocumentProcessor {

    public static final String MAX_PARAGRAPH_LENGTH = "file-processor.default.docx.max-paragraph-length";


    private int maxParagraphLength;

    @Override
    public void init() {
        var apiConfig = ConfigProvider.getInstance().getApiConfig();
        //TODO read config init
        apiConfig.getProperty(MAX_PARAGRAPH_LENGTH);

    }

    @Override
    public boolean support(@NotNull DocumentType type) {
        return type == BuiltinDocumentType.DOCX;
    }

    @Override
    public Stream<DocumentParagraph> split(@NotNull Document document) {
        try (XWPFDocument xwpfDocument = new XWPFDocument(document.getContent().getInputStream())) {
            //TODO process
//            xwpfDocument.getParagraphs()
//                    .stream().map(p->{
//
//                    })
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
