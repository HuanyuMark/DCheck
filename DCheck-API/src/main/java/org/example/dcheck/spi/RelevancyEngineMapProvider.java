package org.example.dcheck.spi;

import lombok.Getter;
import org.example.dcheck.api.ParagraphRelevancyEngine;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Date 2025/02/26
 * get service by a key. supported by MapConfigProvider, find a service class by the associated key and then return its instance
 *
 * @author 三石而立Sunsy
 */
public class RelevancyEngineMapProvider implements DCheckProvider {
    @Getter
    private static final RelevancyEngineMapProvider instance = new RelevancyEngineMapProvider();

    private final InheritableThreadLocal<Map<String, ParagraphRelevancyEngine>> ins = new InheritableThreadLocal<>();

    {
        ins.set(new ConcurrentSkipListMap<>());
        DuplicateCheckingProvider.getInstance().getChecking().onClosing(() -> {
            Map<String, ParagraphRelevancyEngine> map = ins.get();
            if (map != null) {
                map.clear();
            }
        });
    }

    public ParagraphRelevancyEngine getRelevancyEngine(String relevancyEngineKey) {
        return ins.get().computeIfAbsent(relevancyEngineKey, k -> Providers.createService(RelevancyEngineMapConfigProvider.getInstance().getRelevancyEngineMap(), "relevancy engine", k));
    }

    @Nullable
    public ParagraphRelevancyEngine getCurrentDefaultEngine() {
        Collection<ParagraphRelevancyEngine> engines = ins.get().values();
        if (engines.size() == 1) {
            return engines.iterator().next();
        }
        if (engines.isEmpty()) return null;
        throw new IllegalStateException("multiple relevancy engine found: " + engines);
    }
}
