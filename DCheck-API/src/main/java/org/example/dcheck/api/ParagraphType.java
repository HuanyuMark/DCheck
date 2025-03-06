package org.example.dcheck.api;

import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Date 2025/02/26
 * 其实现类在大多数情况下应该是Enum，但是为了拓展性，这里使用接口
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface ParagraphType {

    /**
     * 在实例化ParagraphType时，请将其实例注册到这里，将所有ParagraphType注册到Map中
     * name() -> ParagraphType
     */
    Map<String, ParagraphType> ALL_TYPES = new ConcurrentHashMap<>();

    String name();

    Class<? extends ParagraphMetadata> getMetadataClass();

    Class<? extends Paragraph> getParagraphClass();

    @Nullable
    ParagraphMetadata createExtension(Map<String, Object> all, String documentId, ParagraphLocation location);
}
