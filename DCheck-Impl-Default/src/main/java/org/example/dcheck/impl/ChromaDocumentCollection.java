package org.example.dcheck.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.var;
import org.example.dcheck.api.*;
import org.example.dcheck.spi.DocumentProcessorProvider;
import tech.amikos.chromadb.Collection;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
@Getter
@RequiredArgsConstructor
public class ChromaDocumentCollection implements DocumentCollection {

    protected final ReentrantLock deleteLock = new ReentrantLock();
    private final String id;

    private final ChromaParagraphRelevancyEngine engine;
    private final Collection collection;
    private final Object waitForDeleteMonitor = new Object();
    @Getter
    private volatile boolean exists;

    public ChromaDocumentCollection(Collection collection, ChromaParagraphRelevancyEngine engine) {
        this.id = collection.getName();
        this.collection = collection;
        this.engine = engine;
    }

    @Override
    public void addDocument(List<Document> documents) {
        if (!isExists()) {
            throw new IllegalStateException("Collection not exists");
        }
        waitIfNecessary();
        // ...
        var batch = documents.stream().flatMap(document -> {
            var processor = DocumentProcessorProvider.getInstance().getProcessor(document.getDocumentType());
            return processor.split(document).map(documentParagraph -> {
                if (documentParagraph.getParagraphType() == BuiltinParagraphType.TEXT) {
                    if (!(documentParagraph.getLocation() instanceof TextParagraphLocation)) {
                        throw new IllegalStateException("location must be TextParagraphLocation");
                    }
                    return ParagraphRelevancyCreation.Record.builder()
                            .paragraph(documentParagraph)
                            .metadata(TextParagraphMetadata.builder()
                                    .documentId(document.getId())
                                    .location(((TextParagraphLocation) documentParagraph.getLocation()))
                                    .build())
                            .build();
                }
                //TODO support add other type of paragraph
                throw new UnsupportedOperationException();
            });
        }).collect(Collectors.toList());
        engine.addParagraph(new ParagraphRelevancyCreation(id, batch));
    }

    @Override
    public void deleteDocument(List<String> documentIds) {
        if (!isExists()) {
            throw new IllegalStateException("Collection not exists");
        }
        waitIfNecessary();
        engine.removeDocument(DocumentDelete.builder()
                .collectionId(id)
                .metadataMatchCondition(DocumentDelete.MetadataMatchCondition.builder()
                        .in("documentId", documentIds)
                        .build())
                .build());
    }

    protected void waitIfNecessary() {
        if (deleteLock.isLocked()) {
            try {
                synchronized (waitForDeleteMonitor) {
                    waitForDeleteMonitor.wait();
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException("wait for delete interrupted", e);
            }
        }
    }

    @Override
    public void drop() {
        if (!exists) {
            return;
        }
        deleteLock.lock();
        try {
            if (!exists) {
                return;
            }
            engine.removeDocumentCollection(id);
            exists = false;
        } finally {
            synchronized (waitForDeleteMonitor) {
                waitForDeleteMonitor.notifyAll();
            }
            deleteLock.unlock();
        }
    }
}
