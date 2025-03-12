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
public abstract class LazySplitter implements DCheckDocumentSplitter {

    protected DocumentSplitter delegate = createInitSplitter();

    protected volatile boolean isLoaded;

    @Override
    public List<TextSegment> split(Document document) {
        load();
        return doSplit(document);
    }

    @Override
    public void init() throws Exception {
        if (delegate instanceof DCheckComponent) {
            ((DCheckComponent) delegate).init();
        }
    }

    @Override
    public void inited() throws Exception {
        if (delegate instanceof DCheckComponent) {
            ((DCheckComponent) delegate).inited();
        }
        load();
    }

    private void load() {
        if (!isLoaded) {
            synchronized (this) {
                if (!isLoaded) {
                    delegate = createLoadedSplitter();
                    isLoaded = true;
                }
            }
        }
    }

    @Override
    public List<TextSegment> splitAll(List<Document> documents) {
        load();
        return DCheckDocumentSplitter.super.splitAll(documents);
    }

    protected List<TextSegment> doSplit(Document document) {
        return delegate.split(document);
    }

    protected abstract DocumentSplitter createInitSplitter();

    protected abstract DocumentSplitter createLoadedSplitter();

    @Override
    public String toString() {
        return delegate.toString();
    }
}
