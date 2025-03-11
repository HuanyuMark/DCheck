package org.example.dcheck.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.example.dcheck.api.*;
import org.example.dcheck.spi.ConfigProvider;
import org.example.dcheck.spi.DocumentProcessorProvider;
import org.example.dcheck.spi.RelevancyEngineMapProvider;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@Slf4j
public class DefaultDuplicateChecking implements DuplicateChecking {

    private ParagraphRelevancyEngine relevancyEngine;

    private volatile boolean init;

    @Delegate
    private final IEventEmitter eventEmitter = new ClassHierarchyEventEmitter() {
        @Override
        protected Map<Class<?>, Set<Function<?, CompletableFuture<?>>>> initBus() {
            return new ConcurrentHashMap<>();
        }

        @Override
        protected Set<Function<?, CompletableFuture<?>>> initCallbackSet() {
            return ConcurrentHashMap.newKeySet();
        }
    };

    public ParagraphRelevancyEngine getRelevancyEngine() {
        init();
        return relevancyEngine;
    }

    @Override
    public void init() {
        if (init) return;
        synchronized (this) {
            if (init) return;
            var apiConfig = ConfigProvider.getInstance().getApiConfig();
            relevancyEngine = RelevancyEngineMapProvider.getInstance().getRelevancyEngine(apiConfig.getProperty(ApiConfig.DB_VECTOR_TYPE, ApiConfig.DEFAULT_VALUE));

            try {
                log.info("Starting init Relevancy Engine '{}'", relevancyEngine.getClass().getCanonicalName());
                relevancyEngine.init();
                log.info("Finished init Relevancy Engine");
            } catch (Exception e) {
                throw new IllegalStateException("init relevancy engine fail: " + e.getMessage(), e);
            }


            try {
                log.info("Starting init Document Processors");
                DocumentProcessorProvider.getInstance().init();
                log.info("Finished init Document Processors");
            } catch (Exception e) {
                throw new IllegalStateException("init Document Processors fail: " + e.getMessage(), e);
            }


            try {
                log.info("Call Document Processors hock 'inited()' '{}'", relevancyEngine.getClass().getCanonicalName());
                DocumentProcessorProvider.getInstance().inited();
                log.info("Document Processors 'inited()' hock done");
            } catch (Exception e) {
                throw new IllegalStateException("Document Processors 'inited()' hock throw: " + e.getMessage(), e);
            }

            try {
                log.info("Call Relevancy Engine hock 'inited()' '{}'", relevancyEngine.getClass().getCanonicalName());
                relevancyEngine.inited();
                log.info("Relevancy Engine 'inited()' hock done");
            } catch (Exception e) {
                throw new IllegalStateException("Relevancy Engine 'inited()' hock throw: " + e.getMessage(), e);
            }

            init = true;
        }
    }

    @Override
    public CheckResult check(Check check, DocumentCollection collection) {
        init();
        var queryBuilder = ParagraphRelevancyQuery.builder()
                .documentId(check.getDocument().getId())
                .collectionId(collection.getId())
                .topK(check.getTopKOfEachParagraph());
        // if check.documentId is in collection, we assume queryBuilder.paragraphs() is null
        if (!relevancyEngine.hasDocument(new DocumentIdQuery(collection.getId(), Collections.singletonList(check.getDocument().getId()))).get(0)) {
            queryBuilder
                    .paragraphs(DocumentProcessorProvider.getInstance().split(check.getDocument()).map(p -> (Supplier<Content>) (p::getContent)).collect(Collectors.toList()));
        }
        var queryResult = relevancyEngine.queryParagraph(queryBuilder.build());

        @Getter
        @RequiredArgsConstructor
        class Entry {
            final String documentId;
            final double totalScore;
        }


        return CheckResult.builder()
                .relevantDocuments(
                        queryResult.getRecords().stream()
                                .flatMap(Collection::stream)
                                // calculate total score of each document
                                .map(r -> new CheckResult.RelevantDocument(r.getMetadata().getDocumentId(), r.getRelevancy()))
                                .collect(Collectors.groupingBy(CheckResult.RelevantDocument::getDocumentId))
                                .entrySet()
                                .stream()
                                .map(e -> new Entry(e.getKey(), e.getValue().stream().mapToDouble(CheckResult.RelevantDocument::getScore).sum()))
                                // sort and limit to tokOfDocument
                                .sorted(Comparator.comparingDouble(Entry::getTotalScore))
                                .limit(check.getTopKOfDocument())
                                .map(e -> new CheckResult.RelevantDocument(e.getDocumentId(), e.getTotalScore()))
                                .collect(Collectors.toList())
                )
                .relevantParagraphs(queryResult.getRecords())
                .build();
    }

    @Override
    public void onClosing(Runnable cb) {
        eventEmitter.addSyncListener(CloseEvent.class, e -> cb.run());
    }

    @Override
    public void close() throws Exception {
        if (!init) return;
        synchronized (this) {
            if (!init) return;

            relevancyEngine.close();
            eventEmitter.emitEvent(new CloseEvent()).join();
            eventEmitter.close();

            init = false;
        }
    }

    protected static class CloseEvent {
    }
}
