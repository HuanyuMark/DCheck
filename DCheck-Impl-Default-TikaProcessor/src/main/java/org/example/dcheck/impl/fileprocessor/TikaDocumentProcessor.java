package org.example.dcheck.impl.fileprocessor;

import dev.langchain4j.data.document.DocumentLoader;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.Data;
import org.example.dcheck.api.*;
import org.example.dcheck.impl.CharSeqTextContent;
import org.example.dcheck.impl.ContentMatchParagraphLocation;
import org.example.dcheck.impl.SharedDocumentProcessorConfig;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Date 2025/02/27
 * 支持处理 {@link BuiltinDocumentType#fastValues()} 中的所有文件类型
 *
 * @author 三石而立Sunsy
 */
@Data
public class TikaDocumentProcessor implements DocumentProcessor {

    private DCheckDocumentSplitter splitter;

    private DocumentParser documentParser;

    private volatile boolean init;

    @Override
    public void init() {
        if (init) return;
        synchronized (this) {
            if (init) return;
            documentParser = new ApacheTikaDocumentParser();
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
        return BuiltinDocumentType.fastValues().contains(type);
    }


    @Override
    public Stream<SimpleParagraph> split(@NotNull Document document) {
        init();
        dev.langchain4j.data.document.Document lcDoc = DocumentLoader.load(new DCheckDocumentSource(document), documentParser);
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
