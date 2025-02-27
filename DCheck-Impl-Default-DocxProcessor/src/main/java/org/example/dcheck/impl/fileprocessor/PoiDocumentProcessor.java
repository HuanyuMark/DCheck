package org.example.dcheck.impl.fileprocessor;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.example.dcheck.api.*;
import org.example.dcheck.impl.ContentMatchParagraphLocation;
import org.example.dcheck.impl.TextContent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * Date: 2025/2/27
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public class PoiDocumentProcessor implements DocumentProcessor {

    private int maxParagraphLength;

    @Override
    public void init() throws Exception {
        maxParagraphLength = SharedDocumentProcessorConfig.getInstance().getMaxParagraphLength();
    }

    @Override
    public boolean support(@NotNull DocumentType type) {
        return type == BuiltinDocumentType.DOCX;
    }

    @Override
    public Stream<DocumentParagraph> split(@NotNull Document document) {
        try (XWPFDocument xwpfDocument = new XWPFDocument(document.getContent().getInputStream())) {
            //TODO
            // slice large paragraph into small ones. see max paragraph length config above
            // use llm to rewrite large p to small ones...
            return xwpfDocument.getParagraphs()
                    .stream().map(p -> new TextContent(p.getText()))
                    .map(c -> DocumentParagraph.builder()
                            .paragraphType(BuiltinParagraphType.TEXT)
                            .location(ContentMatchParagraphLocation.get())
                            .content(() -> c)
                            .build());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
