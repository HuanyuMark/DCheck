package org.example.dcheck.impl;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.example.dcheck.api.Codec;
import org.example.dcheck.api.ParagraphLocation;
import org.example.dcheck.api.ParagraphMetadata;
import org.example.dcheck.spi.CodecProvider;
import org.example.dcheck.util.UtilConst;

import java.io.IOException;
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
    private static final Codec codec;

    static {
        codec = CodecProvider.getInstance().getCodecs().stream().findFirst().orElseThrow(() -> new IllegalStateException("not found available codec form CodecProvider. please list a Codec Implementation in classpath"));
    }

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

    protected void forceSyncFieldMap() {
        try {
            Map<String, Object> all = codec.convertTo(this, UtilConst.MAP_TYPE);
            raw.putAll(all);
        } catch (IOException e) {
            throw new IllegalArgumentException("convert to Map<String,Object> fail: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> all() {
        syncFieldMap();
        return raw;
    }
}
