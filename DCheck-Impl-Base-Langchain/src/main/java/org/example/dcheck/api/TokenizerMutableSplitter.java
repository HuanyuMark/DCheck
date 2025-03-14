package org.example.dcheck.api;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.spi.DuplicateCheckingProvider;
import org.example.dcheck.util.UtilConst;

import java.util.List;

/**
 * Date: 2025/3/11
 *
 * @author 三石而立Sunsy
 */
@Setter
@Slf4j
public abstract class TokenizerMutableSplitter extends LazySplitter {

    private DCheckTokenizer tokenizer = DCheckTokenizer.NONE;

    @Override
    public void init() throws Exception {
        tokenizer.init();
        DuplicateCheckingProvider.getInstance().getChecking().addListener(FileProcessorTokenizerInjectionEvent.class, e -> {
            setTokenizer(e.getTokenizer());
            return UtilConst.emptyFuture();
        });
    }

    @Override
    public void inited() throws Exception {
        super.inited();
        tokenizer.inited();
    }

    @Override
    protected List<TextSegment> doSplit(Document document) {
        if (tokenizer != DCheckTokenizer.NONE) {
            delegate = createLoadedSplitter(tokenizer);
        } else {
            log.warn("tokenizer is not injected, use default splitter. to resolve this WARNING, publish '{}' to inject tokenizer after", FileProcessorTokenizerInjectionEvent.class);
        }
        return super.doSplit(document);
    }

    protected abstract DocumentSplitter createLoadedSplitter(DCheckTokenizer tokenizer);
}
