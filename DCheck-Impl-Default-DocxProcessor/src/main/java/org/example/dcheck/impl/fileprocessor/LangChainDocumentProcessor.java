package org.example.dcheck.impl.fileprocessor;

import dev.langchain4j.data.document.DocumentLoader;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import lombok.Data;
import lombok.var;
import org.example.dcheck.api.*;
import org.example.dcheck.impl.ContentMatchParagraphLocation;
import org.example.dcheck.impl.TextContent;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * Date 2025/02/27
 *
 * @author 三石而立Sunsy
 */
@Data
public class LangChainDocumentProcessor implements DocumentProcessor {

    private DocumentSplitter splitter;


    private final DocumentParser documentParser = new ApacheTikaDocumentParser();
    @Override
    public void init() {
        int maxParagraphLength = SharedDocumentProcessorConfig.getInstance().getMaxParagraphLength();
//        int maxOverlaySize = Math.min(maxParagraphLength / 4, 100);
        splitter = DocumentSplitters.recursive(maxParagraphLength, 20);
//        splitter = new DocumentByParagraphSplitter(
//                maxParagraphLength,
//                maxOverlaySize,
//                //TODO define llm splitter to rewrite large segment to small ones
//                new DocumentBySentenceSplitter(maxParagraphLength, maxOverlaySize));
    }

    @Override
    public boolean support(@NotNull DocumentType type) {
        return type == BuiltinDocumentType.DOCX || type == BuiltinDocumentType.PDF;
    }


    @Override
    public Stream<DocumentParagraph> split(@NotNull Document document) {
        var lcDoc = DocumentLoader.load(new DCheckDocumentSource(document), documentParser);
        return splitter.split(lcDoc).stream().map(seg -> {
            // clean ref to seg
            var content = new TextContent(seg.text());
            return DocumentParagraph.builder()
                    // now nowhere to introspect the location, maybe we should define a new splitter to do this
                    .location(ContentMatchParagraphLocation.get())
                    .content(() -> content)
                    .paragraphType(BuiltinParagraphType.TEXT)
                    .build();
        });
    }
}
