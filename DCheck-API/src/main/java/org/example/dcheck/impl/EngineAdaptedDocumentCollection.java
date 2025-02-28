package org.example.dcheck.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.var;
import org.example.dcheck.api.*;
import org.example.dcheck.spi.DocumentProcessorProvider;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
@Getter
@RequiredArgsConstructor
public class EngineAdaptedDocumentCollection implements DocumentCollection {

    private final String id;
    private final ParagraphRelevancyEngine engine;

    protected final Semaphore normalOperationLock = new Semaphore(Integer.MAX_VALUE);
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
        doWithNormalOperationLock(() -> {
            // ...
            var batch = documents.stream().flatMap(document -> DocumentProcessorProvider.getInstance().split(document)
                    .map(documentParagraph -> {
                        if (documentParagraph.getParagraphType() == BuiltinParagraphType.TEXT) {
                            if (!(documentParagraph.getLocation() instanceof TextParagraphLocation)) {
                                throw new IllegalStateException("location must be TextParagraphLocation");
                            }
                            return ParagraphRelevancyCreation.Record.builder()
                                    .paragraph(documentParagraph)
                                    .metadata(TextParagraphMetadata.builder()
                                            .documentId(document.getId())
                                            .location(documentParagraph.getLocation())
                                            .build())
                                    .build();
                        }
                        //TODO support add other type of paragraph
                        throw new UnsupportedOperationException();
                    })).collect(Collectors.toList());
            engine.addParagraph(new ParagraphRelevancyCreation(id, batch));
        });
    }

    @Override
    public void deleteDocument(List<String> documentIds) {
        doWithNormalOperationLock(() -> engine.removeDocument(DocumentDelete.builder()
                .collectionId(id)
                .metadataMatchCondition(MetadataMatchCondition.builder()
                        .in("documentId", documentIds)
                        .build())
                .build()));
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
