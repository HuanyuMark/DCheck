package org.example.dcheck.impl;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.example.dcheck.api.ParagraphLocation;
import org.example.dcheck.api.ParagraphMetadata;
import org.example.dcheck.api.ParagraphType;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Date 2025/02/28
 *
 * @author 三石而立Sunsy
 */
@ToString
@SuperBuilder
@EqualsAndHashCode
public abstract class AbstractParagraphMetadata implements ParagraphMetadata, Map<String, Object> {
    protected final ParagraphType paragraphType = getParagraphType();
    @Getter
    private final String documentId;
    @Getter
    private final ParagraphLocation location;
    @NonNull
    private final HashMap<String, Object> raw = new HashMap<>(4);

    public AbstractParagraphMetadata(String documentId, ParagraphLocation location) {
        this.documentId = documentId;
        this.location = location;
        syncFieldMap();
    }

    public AbstractParagraphMetadata(@Nullable Map<? extends String, ?> m, String documentId, ParagraphLocation location) {
        this.documentId = documentId;
        this.location = location;
        if (m != null) {
            raw.putAll(m);
        }
        syncFieldMap();
    }

    protected void syncFieldMap() {
        if (!raw.isEmpty()) return;
        raw.put("paragraphType", paragraphType);
        raw.put("documentId", documentId);
        raw.put("location", location);
    }

    @Override
    public int size() {
        syncFieldMap();
        return raw.size();
    }

    @Override
    public boolean isEmpty() {
        syncFieldMap();
        return raw.isEmpty();
    }

    @Override
    public Object get(Object key) {
        syncFieldMap();
        return raw.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        syncFieldMap();
        return raw.containsKey(key);
    }

    @Nullable
    @Override
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ?> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object value) {
        syncFieldMap();
        return raw.containsValue(value);
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        syncFieldMap();
        return Collections.unmodifiableSet(raw.keySet());
    }

    @NotNull
    @Override
    public Collection<Object> values() {
        syncFieldMap();
        return Collections.unmodifiableCollection(raw.values());
    }

    @NotNull
    @Override
    public Set<Entry<String, Object>> entrySet() {
        syncFieldMap();
        return Collections.unmodifiableSet(raw.entrySet());
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        syncFieldMap();
        return raw.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super Object> action) {
        syncFieldMap();
        raw.forEach(action);
    }

    @Override
    public Object clone() {
        throw new UnsupportedOperationException();
    }
}
