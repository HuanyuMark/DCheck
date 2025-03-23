package org.example.dcheck.impl.fileprocessor;

import dev.langchain4j.data.document.DocumentLoader;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.example.dcheck.api.*;
import org.example.dcheck.impl.ContentMatchParagraphLocation;
import org.example.dcheck.impl.SharedDocumentProcessorConfig;
import org.example.dcheck.impl.content.CharSeqTextContent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Date 2025/03/06
 *
 * @author 三石而立Sunsy
 */
public class PdfBoxDocumentProcessor implements DocumentProcessor {
    @Getter
    @Setter
    @NonNull
    private DCheckDocumentSplitter splitter;


    private DocumentParser documentParser;

    private volatile boolean init;

    @Override
    public void init() {
        if (init) return;
        synchronized (this) {
            if (init) return;
            documentParser = new ApachePdfBoxDocumentParser();
            int maxParagraphLength = SharedDocumentProcessorConfig.getInstance().getMaxParagraphLength();
//        int maxOverlaySize = Math.min(maxParagraphLength / 4, 100);
            splitter = new TokenizerMutableSplitter() {
                @Override
                protected DocumentSplitter createLoadedSplitter(DCheckTokenizer tokenizer) {
                    return DocumentSplitters.recursive(maxParagraphLength, 20, tokenizer);
                }

                @Override
                protected DocumentSplitter createInitSplitter() {
                    return DocumentSplitters.recursive(maxParagraphLength, 20);
                }
            };

            try {
                splitter.init();
            } catch (Exception e) {
                throw new IllegalStateException("Call DCheckDocumentSplitter hock 'inited()' fail: " + e.getMessage(), e);
            }
//        splitter = new DocumentByParagraphSplitter(
//                maxParagraphLength,
//                maxOverlaySize,
//                //TODO define llm splitter to rewrite large segment to small ones
//                new DocumentBySentenceSplitter(maxParagraphLength, maxOverlaySize));
            init = true;
        }
    }

    @Override
    public void inited() throws Exception {
        splitter.inited();
    }

    @Override
    public boolean support(@NotNull DocumentType type) {
        return BuiltinDocumentType.PDF == type;
    }


    @Override
    public Stream<SimpleParagraph> split(@NotNull Document document) {
        init();
        dev.langchain4j.data.document.Document lcDoc;
        try {
            lcDoc = DocumentLoader.load(new DCheckDocumentSource(document), documentParser);
        } catch (Exception e) {
            throw new IllegalStateException("process document '" + document.getId() + "' fail: " + e.getMessage(), e);
        }
        List<TextSegment> segments = splitter.split(lcDoc);
        return IntStream.range(0, segments.size()).mapToObj(i -> {
            TextSegment seg = segments.get(i);
            // clean ref to seg
            Content content = new CharSeqTextContent(seg.text());
            return SimpleParagraph.builder()
                    // now nowhere to introspect the location, maybe we should define a new splitter to do this
                    .location(ContentMatchParagraphLocation.formLine(seg.text(), i))
                    .content(() -> content)
                    .paragraphType(BuiltinParagraphType.TEXT)
                    .build();
        });
    }
}
