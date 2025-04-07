package org.example.dcheck.api;

import lombok.experimental.ExtensionMethod;
import org.example.dcheck.annotation.Ignore;
import org.example.dcheck.util.BeanProperty;
import org.example.dcheck.util.BeanUtils;
import org.example.dcheck.util.PropertyValue;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Date 2025/03/31
 *
 * @author 三石而立Sunsy
 */
@ExtensionMethod({BeanUtils.class, Collections.class})
public interface EntityProvider<E> {
    Map<Class<?>, List<BeanProperty>> SCHEMA_CACHE = new ConcurrentHashMap<>();

    Map<Class<?>, List<BeanProperty>> POPULATE_CACHE = new ConcurrentHashMap<>();

    static <E> EntityProvider<E> getDefaultProvider(Class<E> type, Supplier<E> factory) {
        return new EntityProvider<E>() {
            @Override
            public E createPlain() {
                return factory.get();
            }

            @Override
            public @NotNull Class<? extends E> getType() {
                return type;
            }
        };
    }

    /**
     * @return properties which has getter and not be annotated with {@link Ignore}
     * and return type of getter is {@link Serializable}
     */
    static List<BeanProperty> getDefaultSchema(Class<?> ruleClazz) {
        return SCHEMA_CACHE.computeIfAbsent(ruleClazz, clazz ->
                clazz.getProperties()
                        .stream()
                        .filter(p -> p.getGetter() != null)
                        .filter(p -> !p.isGetterAnnPresent(Ignore.class))
                        .collect(Collectors.toList())
                        .unmodifiableList()
        );
    }


    default List<BeanProperty> getSchema() {
        return getDefaultSchema(getType());
    }

    default Map<String, PropertyValue> getState(E entity) {
        return getSchema()
                .stream()
                .map(p -> new AbstractMap.SimpleEntry<>(p.getName(), new PropertyValue(p, p.get(entity))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> {
                    throw new IllegalStateException("duplicate state field '" + k1 + "' and '" + k2 + "'");
                }, LinkedHashMap::new))
                .unmodifiableMap();
    }

    @SuppressWarnings("unchecked")
    default E populateStates(E entity, Map<String, PropertyValue> state) {
        List<BeanProperty> setterProperties = POPULATE_CACHE.computeIfAbsent(getClass(), this::computePopulateSchema);
        for (BeanProperty property : setterProperties) {
            PropertyValue propertyValue = state.get(property.getName());
            if (propertyValue == null) {
                continue;
            }
            if (property.getPropertyType().isInstance(propertyValue.getValue())) {
                if (property.getSetter() != null) {
                    property.set(entity, propertyValue.getValue());
                } else if (property.getWither() != null) {
                    entity = (E) property.with(entity, propertyValue.getValue());
                }
            } else {
                throw new IllegalArgumentException("restore property '" + property.getName() + "' fail: expected type is '" + property.getPropertyType() + "'");
            }
        }
        return entity;
    }

    default List<BeanProperty> computePopulateSchema(Class<?> clazz) {
        return clazz
                .getProperties()
                .stream()
                .filter(p -> !p.isSetterAnnPresent(Ignore.class) || !p.isWitherAnnPresent(Ignore.class))
                .collect(Collectors.toList())
                .unmodifiableList();
    }

    E createPlain();

    @NotNull
    Class<? extends E> getType();
}
