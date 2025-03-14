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

    protected volatile boolean inited = false;

    @Override
    public List<TextSegment> split(Document document) {
        return doSplit(document);
    }

    @Override
    public void init() throws Exception {
        if (inited) return;
        synchronized (this) {
            if (inited) return;

            if (delegate instanceof DCheckComponent) {
                ((DCheckComponent) delegate).init();
            }

            inited = true;
        }
    }

    @Override
    public void inited() throws Exception {
        if (delegate instanceof DCheckComponent) {
            ((DCheckComponent) delegate).inited();
        }
        delegate = createLoadedSplitter();
    }

    @Override
    public List<TextSegment> splitAll(List<Document> documents) {
        return delegate.splitAll(documents);
    }

    protected List<TextSegment> doSplit(Document document) {
        return delegate.split(document);
    }

    protected abstract DocumentSplitter createInitSplitter();

    protected DocumentSplitter createLoadedSplitter() {
        return delegate;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
