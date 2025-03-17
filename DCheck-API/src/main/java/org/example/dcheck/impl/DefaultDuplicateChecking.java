package org.example.dcheck.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.api.*;
import org.example.dcheck.spi.ConfigProvider;
import org.example.dcheck.spi.DocumentProcessorProvider;
import org.example.dcheck.spi.RelevancyEngineMapProvider;
import org.example.dcheck.support.PreloadClassLoader;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@Slf4j
public class DefaultDuplicateChecking implements DuplicateChecking {

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
    private ParagraphRelevancyEngine relevancyEngine;
    private volatile boolean init;

    public ParagraphRelevancyEngine getRelevancyEngine() {
        init();
        return relevancyEngine;
    }

    @Override
    public WhiteListManager getWhiteListManager() {
        throw new UnsupportedOperationException("TODO impl...");
    }

    @Override
    public void init() {
        if (init) return;
        synchronized (this) {
            new PreloadClassLoader().perform();

            if (init) return;
            ApiConfig apiConfig = ConfigProvider.getInstance().getApiConfig();
            relevancyEngine = RelevancyEngineMapProvider.getInstance().getRelevancyEngine(apiConfig.required(ApiConfig.DB_VECTOR_TYPE, ApiConfig.DEFAULT_VALUE));

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
        ParagraphRelevancyQuery.ParagraphRelevancyQueryBuilder queryBuilder = ParagraphRelevancyQuery.builder()
                .documentId(check.getDocument().getId())
                .collectionId(collection.getId())
                .minRelevancy(check.getMinParagraphRelevancy())
                .topK(check.getTopKOfEachParagraph());
        // if check.documentId is in collection, we assume queryBuilder.paragraphs() is null
        if (!relevancyEngine.hasDocument(new DocumentIdQuery(collection.getId(), Collections.singletonList(check.getDocument().getId()))).get(0)) {
            queryBuilder
                    .paragraphs(DocumentProcessorProvider.getInstance().splitToParagraphs(check.getDocument()).collect(Collectors.toList()));
        }
        ParagraphRelevancyQueryResult queryResult = relevancyEngine.queryParagraph(queryBuilder.build());

        @Getter
        @RequiredArgsConstructor
        class Entry {
            final String documentId;
            final double totalScore;
        }


        return CheckResult.builder()
                .relevantDocuments(
                        queryResult.getDuplicateParts()
                                .stream()
                                .flatMap(p -> p.getDuplicates().stream())
                                // calculate total score of each document
                                .map(r -> new CheckResult.RelevantDocument(r.getMetadata().getDocumentId(), r.getRelevancy()))
                                .collect(Collectors.groupingBy(CheckResult.RelevantDocument::getDocumentId))
                                .entrySet()
                                .stream()
                                .map(e -> new Entry(e.getKey(), e.getValue().stream().mapToDouble(CheckResult.RelevantDocument::getScore).sum()))
                                .filter(e -> e.getTotalScore() >= check.getMinDocumentRelevancy())
                                // sort and limit to tokOfDocument
                                .sorted(Comparator.comparingDouble(Entry::getTotalScore).reversed())
                                .limit(check.getTopKOfDocument())
                                .map(e -> new CheckResult.RelevantDocument(e.getDocumentId(), e.getTotalScore()))
                                .collect(Collectors.toList())
                )
                .duplicateParts(queryResult.getDuplicateParts())
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

    @Override
    public <E> void addListener(Class<E> event, Function<E, @NotNull CompletableFuture<?>> listener) {
        eventEmitter.addListener(event, listener);
    }

    @Override
    public <E> void addOnceListener(Class<E> event, Function<E, @NotNull CompletableFuture<?>> listener) {
        eventEmitter.addOnceListener(event, listener);
    }

    @Override
    public <E> void addSyncListener(Class<E> event, Consumer<E> cb) {
        eventEmitter.addSyncListener(event, cb);
    }

    @Override
    public <E> void removeListener(Class<E> event, Function<E, @NotNull CompletableFuture<?>> listener) {
        eventEmitter.removeListener(event, listener);
    }

    @Override
    public <E> void removeSyncListener(Class<E> event, Consumer<E> cb) {
        eventEmitter.removeSyncListener(event, cb);
    }

    @Override
    public CompletableFuture<?> emitEvent(Object event) {
        return eventEmitter.emitEvent(event);
    }

    @Override
    public <T> CompletableFuture<?> emitEvent(Class<T> evnetClass, T event) {
        return eventEmitter.emitEvent(evnetClass, event);
    }

    protected static class CloseEvent {
    }
}
