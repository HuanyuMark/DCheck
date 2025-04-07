package org.example.dcheck.util;

import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Date 2025/03/31
 *
 * @author 三石而立Sunsy
 */
@ToString
@EqualsAndHashCode
public class BeanProperty {

    @With
    @Getter
    @NotNull
    private final Class<?> beanType;

    @Getter
    private final String name;

    /**
     * {@code Object getMyProperty()}
     */
    @With
    @Getter
    @Nullable
    private final Method getter;

    /**
     * maybe
     * <p>
     * {@link SetterMode#ACCESSOR} setter, method signature:
     * <p>
     * {@code Self setMyProperty(Object value)}
     * <p>
     * or
     * <p>
     * {@link SetterMode#NONE_SETTER} setter, method signature:
     * <p>
     * {@code void setMyProperty(Object value)}
     *
     * @see #getSetterMode()
     */
    @With
    @Getter
    @Nullable
    private final Method setter;

    /**
     * {@code Self withMyProperty(Object value)}
     */
    @With
    @Getter
    @Nullable
    private final Method wither;

    @With
    @Getter
    @Nullable
    private final Field field;

    @With
    @Getter
    private final Class<?> propertyType;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    protected final Map<AnnotatedElement, AnnotationCache> annotationCaches = new ConcurrentHashMap<>(7);

    protected BeanProperty(@NonNull Class<?> beanType, @NonNull String name, @Nullable Method getter, @Nullable Method setter, @Nullable Method wither, @NonNull Class<?> propertyType) {
        this(beanType, name, getter, setter, wither, getField(beanType, name), propertyType);
    }

    protected BeanProperty(@NonNull Class<?> beanType, String name, @Nullable Method getter, @Nullable Method setter, @Nullable Method wither, @Nullable Field field, Class<?> propertyType) {
        this.beanType = beanType;
        this.name = name;
        this.getter = getter;
        this.setter = setter;
        this.wither = wither;
        this.field = field;
        this.propertyType = propertyType;
    }

    private static @Nullable Field getField(Class<?> beanType, String name) {
        Field f = ReflectionUtils.findField(beanType, name);
        if (f != null) {
            f.setAccessible(true);
        }
        return f;
    }

    @Nullable
    public <A extends Annotation> A getSetterAnn(Class<A> annotationClass) {
        return setter == null ? null : annotationCaches.computeIfAbsent(setter, AnnotationCache::of).get(annotationClass);
    }

    @Nullable
    public <A extends Annotation> A getGetterAnn(Class<A> annotationClass) {
        return getter == null ? null : annotationCaches.computeIfAbsent(getter, AnnotationCache::of).get(annotationClass);
    }

    @Nullable
    public <A extends Annotation> A getWitherAnn(Class<A> annotationClass) {
        return wither == null ? null : annotationCaches.computeIfAbsent(wither, AnnotationCache::of).get(annotationClass);
    }

    @Nullable
    public <A extends Annotation> A getFieldAnn(Class<A> annotationClass) {
        return field == null ? null : annotationCaches.computeIfAbsent(field, AnnotationCache::of).get(annotationClass);
    }

    public boolean isAnyAnnPresent(Class<? extends Annotation> annotationClass) {
        if (isSetterAnnPresent(annotationClass)) return true;
        if (isGetterAnnPresent(annotationClass)) return true;
        if (isWitherAnnPresent(annotationClass)) return true;
        return isFieldAnnPresent(annotationClass);
    }

    public boolean isGetterAnnPresent(Class<? extends Annotation> annotationClass) {
        return getGetterAnn(annotationClass) != null;
    }

    public boolean isSetterAnnPresent(Class<? extends Annotation> annotationClass) {
        return getSetterAnn(annotationClass) != null;
    }

    public boolean isWitherAnnPresent(Class<? extends Annotation> annotationClass) {
        return getWitherAnn(annotationClass) != null;
    }

    public boolean isFieldAnnPresent(Class<? extends Annotation> annotationClass) {
        return getFieldAnn(annotationClass) != null;
    }

    @NotNull
    public SetterMode getSetterMode() {
        if (setter == null) return SetterMode.NONE_SETTER;
        if (beanType.isAssignableFrom(setter.getReturnType())) {
            return SetterMode.ACCESSOR;
        }
        return SetterMode.STANDARD;
    }

    public Object get(Object target) {
        if (getter == null) {
            return null;
        }
        return ReflectionUtils.invokeMethod(getter, target);
    }

    public Object getFromField(Object target) {
        if (field == null) return null;
        return ReflectionUtils.getField(field, target);
    }

    public void set(Object target, Object value) {
        if (setter == null) return;
        ReflectionUtils.invokeMethod(setter, target, value);
    }

    public void setByField(Object target, Object value) {
        if (field == null) return;
        ReflectionUtils.setField(field, target, value);
    }

    public Object with(Object target, Object value) {
        if (wither == null) return target;
        return ReflectionUtils.invokeMethod(wither, target, value);
    }

    public enum SetterMode {
        /**
         * {@code Self_Or_BaseTypeOfSelf setMyProperty(Object value)}
         */
        ACCESSOR,
        /**
         * {@code void setMyProperty(Object value)}
         */
        STANDARD,
        /**
         * associated {@link BeanProperty} have no setter
         */
        NONE_SETTER
    }
}
