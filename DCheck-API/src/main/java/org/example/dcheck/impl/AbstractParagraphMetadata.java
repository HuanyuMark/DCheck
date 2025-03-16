package org.example.dcheck.impl;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.example.dcheck.api.ParagraphLocation;
import org.example.dcheck.api.ParagraphMetadata;
import org.springframework.cglib.beans.BeanMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Date 2025/02/28
 *
 * @author 三石而立Sunsy
 */
@SuperBuilder
@EqualsAndHashCode
public abstract class AbstractParagraphMetadata implements ParagraphMetadata {
    @NonNull
    protected final Map<String, Object> raw = new HashMap<>(getRawInitialCapacity());
    @Getter
    private final String documentId;
    @Getter
    private final ParagraphLocation location;

    public AbstractParagraphMetadata(String documentId, ParagraphLocation location) {
        this.documentId = documentId;
        this.location = location;
        syncFieldMap();
    }

    protected int getRawInitialCapacity() {
        return 6;
    }

    @Override
    public String toString() {
        return raw.toString();
    }

    public abstract AbstractParagraphMetadata withOthers(Map<String, ?> rawMetadata);

    protected void syncFieldMap() {
        if (!raw.isEmpty()) return;
        forceSyncFieldMap();
    }

    @SuppressWarnings("unchecked")
    protected void forceSyncFieldMap() {
        BeanMap.Generator gen = new BeanMap.Generator();
        gen.setBean(this);
        gen.setRequire(BeanMap.REQUIRE_GETTER);
        raw.putAll(gen.create());
    }

    @Override
    public Map<String, Object> all() {
        syncFieldMap();
        return raw;
    }
}
