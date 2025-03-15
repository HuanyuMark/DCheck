package org.example.dcheck.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.dcheck.api.*;
import org.example.dcheck.spi.DocumentProcessorProvider;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
@Getter
@RequiredArgsConstructor
public class EngineAdaptedDocumentCollection implements DocumentCollection {

    protected final Semaphore normalOperationLock = new Semaphore(Integer.MAX_VALUE);
    private final String id;
    private final ParagraphRelevancyEngine engine;
    @Getter
    private volatile boolean exists = true;

    protected void doWithNormalOperationLock(Runnable runnable) {
        ensureOps();
        try {
            normalOperationLock.acquire();
        } catch (InterruptedException e) {
            throw new IllegalStateException("acquire normal operation fail: ", e);
        }
        try {
            ensureOps();
            runnable.run();
        } finally {
            normalOperationLock.release();
        }
    }

    @Override
    public void addDocument(List<Document> documents) {
        List<Boolean> added = hasDocument(documents.stream().map(Document::getId).collect(Collectors.toList()));

        List<UniversalParagraph> batch = IntStream.range(0, documents.size())
                .filter(i -> !added.get(i))
                .mapToObj(documents::get)
                .flatMap(document -> DocumentProcessorProvider
                        .getInstance()
                        .splitToParagraphs(document)
                ).collect(Collectors.toList());

        if (batch.isEmpty()) {
            return;
        }

        doWithNormalOperationLock(() -> engine.addParagraph(new ParagraphRelevancyCreation(id, batch)));
    }

    @Override
    public void deleteDocument(Set<String> documentIds) {
        doWithNormalOperationLock(() -> engine.removeDocument(DocumentDelete.builder()
                .collectionId(id)
                .metadataMatchCondition(MetadataMatchCondition.builder()
                        .in("documentId", documentIds)
                        .build())
                .build()));
    }

    @Override
    public List<Boolean> hasDocument(List<String> documentIds) {
        return engine.hasDocument(new DocumentIdQuery(id, documentIds));
    }

    protected void ensureOps() {
        if (isExists()) return;
        throw new IllegalStateException("Collection not exists");
    }

    @Override
    public void drop() {
        if (!exists) {
            return;
        }
        try {
            normalOperationLock.acquire(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
            throw new IllegalStateException("drop document collection sync fail:", e);
        }
        try {
            if (!exists) {
                return;
            }
            engine.removeDocumentCollection(id);
            exists = false;
        } finally {
            normalOperationLock.release(Integer.MAX_VALUE);
        }
    }
}
