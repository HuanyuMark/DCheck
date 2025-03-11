package org.example.dcheck.api;

import dev.langchain4j.data.document.DocumentSplitter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.spi.DuplicateCheckingProvider;
import org.example.dcheck.util.UtilConst;

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
        tokenizer.inited();
    }

    @Override
    protected DocumentSplitter createLoadedSplitter() {
        if (tokenizer != DCheckTokenizer.NONE) {
            return createLoadedSplitter(tokenizer);
        } else {
            log.warn("tokenizer is not injected, use default splitter");
        }
        return delegate;
    }

    protected abstract DocumentSplitter createLoadedSplitter(DCheckTokenizer tokenizer);
}
