package org.example.dcheck.util;

import lombok.NonNull;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Date 2025/03/31
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public class BeanUtils {

    protected static final Map<Class<?>, List<BeanProperty>> propertyCache = new ConcurrentHashMap<>();

    protected static final Map<Class<?>, Map<String, BeanProperty>> propertyMapCache = new ConcurrentHashMap<>();

    protected static String getPropertyName(Method method, int nameStartIdx) {
        String rawName = method.getName().substring(nameStartIdx);
        if (Character.isLowerCase(rawName.charAt(0))) return rawName;
        StringBuilder str = new StringBuilder(rawName);
        str.replace(0, 1, String.valueOf(Character.toLowerCase(str.charAt(0))));
        return str.toString();
    }

    public static List<BeanProperty> getProperties(Class<?> clazz) {
        return propertyCache.computeIfAbsent(clazz, target -> {
            Map<String, BeanProperty> properties = new LinkedHashMap<>();
            Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(target, method -> !ReflectionUtils.isObjectMethod(method));
            for (Method method : methods) {
                if (!Modifier.isPublic(method.getModifiers())) continue;
                if (Modifier.isAbstract(method.getModifiers())) continue;

                boolean isGetterName;
                boolean isSetterName = false;
                boolean isWitherName = false;

                String propertyName;
                if ((isGetterName = method.getName().startsWith("get")) || (isSetterName = method.getName().startsWith("set"))) {
                    propertyName = getPropertyName(method, 3);
                } else if (method.getName().startsWith("is")) {
                    isGetterName = true;
                    propertyName = getPropertyName(method, 2);
                } else if (method.getName().startsWith("with")) {
                    isWitherName = true;
                    propertyName = getPropertyName(method, 4);
                } else {
                    continue;
                }

                if (isGetterName && method.getReturnType() != void.class && method.getParameterCount() == 0) {
                    BeanProperty property = properties.get(propertyName);
                    if (property == null) {
                        properties.put(propertyName, new BeanProperty(target, propertyName, method, null, null, method.getReturnType()));
                    } else if (property.getSetter() == null && property.getPropertyType().isAssignableFrom(method.getReturnType())) {
                        properties.put(propertyName, property.withGetter(method));
                    }
                } else if (isSetterName && method.getParameterCount() == 1) {
                    BeanProperty property = properties.get(propertyName);
                    Class<?> valueType = method.getParameterTypes()[0];
                    if (property == null) {
                        properties.put(propertyName, new BeanProperty(target, propertyName, null, method, null, valueType));
                    } else if (property.getSetter() != null) {
                        continue;
                    } else if (valueType.isAssignableFrom(property.getPropertyType())) {
                        properties.put(propertyName, property.withSetter(method).withPropertyType(valueType));
                    }
                } else if (isWitherName && method.getParameterCount() == 1 && method.getReturnType().isAssignableFrom(target)) {
                    BeanProperty property = properties.get(propertyName);
                    Class<?> valueType = method.getParameterTypes()[0];
                    if (property == null) {
                        properties.put(propertyName, new BeanProperty(target, propertyName, null, null, method, valueType));
                    } else if (property.getWither() != null) {
                        continue;
                    } else if (valueType.isAssignableFrom(property.getPropertyType())) {
                        properties.put(propertyName, property.withWither(method).withPropertyType(valueType));
                    }
                } else {
                    throw new IllegalStateException();
                }
                method.setAccessible(true);
            }
            return Collections.unmodifiableList(new ArrayList<>(properties.values()));
        });
    }

    public static Map<String, BeanProperty> getPropertiesMap(Class<?> clazz) {
        return propertyMapCache.computeIfAbsent(clazz, target -> Collections.unmodifiableMap(getProperties(target).stream().collect(Collectors.toMap(BeanProperty::getName, Function.identity(), (a, b) -> {
            throw new IllegalArgumentException(
                    String.format("Duplicate property name [%s]", a.getName())
            );
        }, LinkedHashMap::new))));
    }

    public void clearCache() {
        propertyCache.clear();
        propertyMapCache.clear();
    }

    /**
     * @return target
     */
    public <E> E copyProperties(@NonNull Object source, E target) {
        if (target == null) return null;

        List<BeanProperty> sourceProperty = getProperties(source.getClass());
        Map<String, BeanProperty> targetProperty = getPropertiesMap(target.getClass());
        for (BeanProperty property : sourceProperty) {
            BeanProperty targetPro = targetProperty.get(property.getName());
            if (targetPro == null) continue;
            // todo impl: predicate.test() and check value type ...
            targetPro.set(target, property.get(source));
        }
        return target;
    }
}
