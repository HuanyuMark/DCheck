package org.example.dcheck.impl;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.api.*;
import org.example.dcheck.spi.DCheckConfigProvider;
import org.example.dcheck.spi.DocumentProcessorProvider;
import org.example.dcheck.spi.RelevancyEngineMapProvider;
import org.example.dcheck.support.PreloadClassLoader;
import org.example.dcheck.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.*;
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

    public static final int WHITE_LIST_RULE_CAL_CHUNK_SIZE = 15;
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
    public Optional<WhiteListManager> getWhiteListManager() {
        // TODO impl...
        return Optional.empty();
    }

    @Override
    public void init() {
        if (init) return;
        synchronized (this) {
            new PreloadClassLoader().perform();

            if (init) return;
            DCheckConfig config = DCheckConfigProvider.getInstance().getDCheckConfig();
            relevancyEngine = RelevancyEngineMapProvider.getInstance().createRelevancyEngine(config.required(DCheckConfig.DB_VECTOR_TYPE, DCheckConfig.DEFAULT_VALUE));

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

            if (config.requiredEnable(DCheckConfig.DCHECK_CONFIG_AUTO_CLEAR)) {
                DCheckConfigProvider.getInstance().getInjectedConfigResources().clear();
            } else {
                log.warn("'{}' is disabled, please clear injected config resources manually", DCheckConfig.DCHECK_CONFIG_AUTO_CLEAR);
            }

            init = true;
        }
    }


    @Override
    public CheckResult check(@NonNull Check check, @NonNull DocumentCollection collection) {
        init();

        ParagraphRelevancyQuery query = buildQuery(check, collection);

        ParagraphRelevancyQueryResult queryResult = relevancyEngine.queryParagraph(query);

        ParagraphRelevancyQueryResult postProcessResult = postProcessResult(check, queryResult);

        return CheckResult.builder()
                .relevantDocuments(doDocumentStatistics(check, postProcessResult))
                .duplicateParts(postProcessResult.getDuplicateParts())
                .build();
    }


    protected ParagraphRelevancyQuery buildQuery(@NotNull Check check, @NotNull DocumentCollection collection) {
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
        return queryBuilder.build();
    }

    protected ParagraphRelevancyQueryResult postProcessResult(@NotNull Check check, ParagraphRelevancyQueryResult queryResult) {
        if (check.getWhiteLists().isEmpty()) return queryResult;

        log.debug("Apply white list rule to document '{}': {}", check.getDocument().getId(), check.getWhiteLists().stream().map(WhiteListRuleSet::getId).collect(Collectors.toList()));

        WhiteListRule.FilterContext filterContext = new DefaultFilterContext(check);

        return queryResult.withDuplicateParts(
                CollectionUtils.partition(queryResult.getDuplicateParts(), WHITE_LIST_RULE_CAL_CHUNK_SIZE).stream()
                        .flatMap(ps -> check.getWhiteLists()
                                .stream()
                                .flatMap(WhiteListRuleSet::getEnabledRules)
                                .reduce(
                                        ps,
                                        (duplicateParts, rule) -> rule.calculateFilterScore(duplicateParts, filterContext),
                                        (prev, nest) -> {
                                            //must be run in sequential stream (ensure the filter chain of each rule). the combiner cannot be used in subtask merging
                                            throw new IllegalStateException();
                                        }
                                )
                                .stream())
                        .collect(Collectors.toList())
        );
    }

    @NotNull
    protected List<CheckResult.RelevantDocument> doDocumentStatistics(@NotNull Check check, ParagraphRelevancyQueryResult postProcessResult) {
        @Getter
        @RequiredArgsConstructor
        class DocumentTotalScoreEntry {
            final String documentId;
            final double totalScore;
        }

        return postProcessResult.getDuplicateParts()
                .stream()
                .flatMap(p -> p.getDuplicates().stream())
                // calculate total score of each document
                .map(r -> new CheckResult.RelevantDocument(r.getMetadata().getDocumentId(), r.getRelevancy()))
                .collect(Collectors.groupingBy(CheckResult.RelevantDocument::getDocumentId))
                .entrySet()
                .stream()
                .map(e -> new DocumentTotalScoreEntry(e.getKey(), e.getValue().stream().mapToDouble(CheckResult.RelevantDocument::getScore).sum()))
                .filter(e -> e.getTotalScore() >= check.getMinDocumentRelevancy())
                // sort and limit to tokOfDocument
                .sorted(Comparator.comparingDouble(DocumentTotalScoreEntry::getTotalScore).reversed())
                .limit(check.getTopKOfDocument())
                .map(e -> new CheckResult.RelevantDocument(e.getDocumentId(), e.getTotalScore()))
                .collect(Collectors.toList());
    }

    @Override
    public void onClosing(@NonNull Runnable cb) {
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

    @Value
    protected static class DefaultFilterContext implements WhiteListRule.FilterContext {
        Check check;

        @Override
        public boolean isFiltered(@Range(from = 0, to = 1) double score) {
            return score > check.getWhiteListThreshold();
        }
    }
}
