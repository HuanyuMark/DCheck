package org.example.dcheck.api;

import org.example.dcheck.util.BeanProperty;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

/**
 * Date: 2025/3/19
 * support type hint
 *
 * @author 三石而立Sunsy
 */
public interface AllowListRuleType extends Serializable, EntityProvider<AllowListRule> {

    /**
     * same as {@link ParagraphType} and {@link ParagraphLocationType}.
     * <p>
     * support codec work
     */
    Map<String, AllowListRuleType> ALL_TYPES = new ConcurrentSkipListMap<>();

    Map<Class<? extends AllowListRuleType>, List<BeanProperty>> SCHEMA_CACHE = new ConcurrentSkipListMap<>();

    @NotNull
    String name();

    @Override
    default List<BeanProperty> getSchema() {
        //TODO 改成cache
        return EntityProvider.super.getSchema()
                .stream().filter(p -> Serializable.class.isAssignableFrom(p.getPropertyType()))
                .collect(Collectors.toList());
    }
}
