package org.example.dcheck.impl.fileprocessor;

import lombok.var;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.example.dcheck.api.*;
import org.example.dcheck.impl.ContentMatchParagraphLocation;
import org.example.dcheck.impl.InMemoryTextContent;
import org.example.dcheck.impl.SharedDocumentProcessorConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
            var contents = xwpfDocument.getParagraphs()
                    .stream()
                    .map(XWPFParagraph::getText)
                    .filter(StringUtils::hasText)
                    .map(InMemoryTextContent::new)
                    .collect(Collectors.toList());

            return IntStream.range(0, contents.size())
                    .mapToObj(i -> {
                        var content = contents.get(i);
                        return DocumentParagraph.builder()
                                .paragraphType(BuiltinParagraphType.TEXT)
                                .location(ContentMatchParagraphLocation.formLine(content.getText().toString(), i))
                                .content(() -> content)
                                .build();
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
