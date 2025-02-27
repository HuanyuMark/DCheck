package org.example.dcheck.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.var;
import org.example.dcheck.api.*;
import org.example.dcheck.spi.ConfigProvider;
import org.example.dcheck.spi.DocumentProcessorProvider;
import org.example.dcheck.spi.MapSpi;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
public class DefaultDuplicateChecking implements DuplicateChecking {

    @Getter
    private ParagraphRelevancyEngine relevancyEngine;

    @Override
    public void init() {
        var apiConfig = ConfigProvider.getInstance().getApiConfig();
        relevancyEngine = MapSpi.getInstance().getRelevancyEngine(apiConfig.getProperty(ApiConfig.DB_VECTOR_TYPE, ApiConfig.DEFAULT_VALUE));

        try {
            relevancyEngine.init();
        } catch (Exception e) {
            throw new IllegalStateException("init relevancy engine fail:", e);
        }
    }

    @Override
    public CheckResult check(Check check, DocumentCollection collection) {
        var queryResult = relevancyEngine.queryParagraph(
                ParagraphRelevancyQuery.builder()
                        .collectionId(collection.getId())
                        .topK(check.getTopKOfEachParagraph())
                        .paragraphs(DocumentProcessorProvider.getInstance().split(check.getDocument()).map(p -> (Supplier<Content>) (p::getContent)).collect(Collectors.toList()))
                        .build()
        );

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
                                .map(r -> new CheckResult.RelevantDocument(r.getDocumentId(), r.getRelevancy()))
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
}
