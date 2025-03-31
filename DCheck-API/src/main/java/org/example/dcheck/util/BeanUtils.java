package org.example.dcheck.util;

import org.example.dcheck.api.BeanProperty;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Date 2025/03/31
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public class BeanUtils {

    protected static final Map<Class<?>, List<BeanProperty>> propertyCache = new ConcurrentHashMap<>();

    protected static String getPropertyName(Method method, int nameStartIdx) {
        StringBuilder str = new StringBuilder(method.getName().substring(nameStartIdx));
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
                    StringBuilder str = new StringBuilder(method.getName().substring(2));
                    str.replace(0, 1, String.valueOf(Character.toLowerCase(str.charAt(0))));
                    propertyName = getPropertyName(method, 2);
                } else if (method.getName().startsWith("with")) {
                    isWitherName = true;
                    propertyName = getPropertyName(method, 4);
                } else {
                    continue;
                }

                if (isGetterName && method.getReturnType() != Void.class && method.getParameterCount() == 0) {
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
                } else if (isWitherName && method.getParameterCount() == 1 && target.isAssignableFrom(method.getReturnType())) {
                    BeanProperty property = properties.get(propertyName);
                    Class<?> valueType = method.getParameterTypes()[0];
                    if (property == null) {
                        properties.put(propertyName, new BeanProperty(target, propertyName, null, null, method, valueType));
                    } else if (property.getSetter() != null) {
                        continue;
                    } else if (valueType.isAssignableFrom(property.getPropertyType())) {
                        properties.put(propertyName, property.withSetter(method).withPropertyType(valueType));
                    }
                } else {
                    throw new IllegalStateException();
                }
                method.setAccessible(true);
            }
            return Collections.unmodifiableList(new ArrayList<>(properties.values()));
        });
    }

    public void clearCache() {
        propertyCache.clear();
    }
}
