package org.example.dcheck.impl;

import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.example.dcheck.api.DocumentCollection;
import org.example.dcheck.api.ParagraphRelevancyEngine;
import org.example.dcheck.api.TempDocumentCollection;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Date: 2025/2/28
 *
 * @author 三石而立Sunsy
 */
@Slf4j
public abstract class AbstractParagraphRelevancyEngine implements ParagraphRelevancyEngine {

    protected final Set<TempDocumentCollection> tempDocumentCollections = ConcurrentHashMap.newKeySet();

    protected volatile boolean init;

    @Override
    public void init() {
        if (init) {
            return;
        }
        synchronized (this) {
            if (init) {
                return;
            }

            doInit();

            // Backup plan: clean temp document collection
            Runtime.getRuntime().addShutdownHook(new Thread() {
                {
                    setName(AbstractParagraphRelevancyEngine.this.getClass().getName() + "::shutdownHook");
                }

                @Override
                public void run() {
                    if (tempDocumentCollections.isEmpty()) {
                        log.info("[TempDocumentCollection Leak Detection]: all collections has been closed, good job!");
                        return;
                    }
                    log.warn("[TempDocumentCollection Leak Detection]: remember closing the collection after using (try-with-resources statement is best practice)");
                    for (TempDocumentCollection collection : tempDocumentCollections) {
                        try {
                            collection.close();
                        } catch (Exception e) {
                            log.warn("close leaked collection fail: {}", e.getMessage(), e);
                        }
                    }
                }
            });
            init = true;
        }
    }

    protected void doInit() {
    }

    @Override
    public TempDocumentCollection newTempDocumentCollection() {
        init();
        var co = new TempDocumentCollectionAdaptor(doNewTempDocumentCollection()) {
            @Override
            public void drop() {
                super.drop();
                tempDocumentCollections.remove(this);
            }
        };
        tempDocumentCollections.add(co);
        return co;
    }

    protected DocumentCollection doNewTempDocumentCollection() {
        return getOrCreateDocumentCollection(generateTempDocumentCollectionId());
    }

    protected String generateTempDocumentCollectionId() {
        return UUID.randomUUID().toString();
    }
}
