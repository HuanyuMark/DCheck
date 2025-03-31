package org.example.dcheck.api;

import lombok.*;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.annotation.AnnotationUtils;
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
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BeanProperty {

    @With
    @Getter
    private final Class<?> beanType;

    @Getter
    private final String name;
    @With
    @Getter
    @Nullable
    private final Method getter;
    @With
    @Getter
    @Nullable
    private final Method setter;
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
    @ToString.Include
    @EqualsAndHashCode.Include
    private final Class<?> propertyType;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    protected final Map<AnnotatedElement, AnnotationCache> annotationCaches = new ConcurrentHashMap<>(7);

    @RequiredArgsConstructor
    protected static class AnnotationCache {
        private final AnnotatedElement annotatedElement;
        private final Map<Class<? extends Annotation>, Annotation> cache = new ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        public <A extends Annotation> A get(Class<A> annotationClass) {
            return ((A) cache.computeIfAbsent(annotationClass, ann -> AnnotationUtils.findAnnotation(annotatedElement, ann)));
        }
    }

    public BeanProperty(Class<?> beanType, String name, @Nullable Method getter, @Nullable Method setter, @Nullable Method wither, Class<?> propertyType) {
        this(beanType, name, getter, setter, wither, getField(beanType, name), propertyType);
    }

    private static @Nullable Field getField(Class<?> beanType, String name) {
        Field f = ReflectionUtils.findField(beanType, name);
        if (f != null) {
            f.setAccessible(true);
        }
        return f;
    }

    public BeanProperty(Class<?> beanType, String name, @Nullable Method getter, @Nullable Method setter, @Nullable Method wither, @Nullable Field field, Class<?> propertyType) {
        this.beanType = beanType;
        this.name = name;
        this.getter = getter;
        this.setter = setter;
        this.wither = wither;
        this.field = field;
        this.propertyType = propertyType;
    }

    @Nullable
    public <A extends Annotation> A getSetterAnn(Class<A> annotationClass) {
        return setter == null ? null : annotationCaches.computeIfAbsent(setter, AnnotationCache::new).get(annotationClass);
    }

    @Nullable
    public <A extends Annotation> A getGetterAnn(Class<A> annotationClass) {
        return getter == null ? null : annotationCaches.computeIfAbsent(getter, AnnotationCache::new).get(annotationClass);
    }

    @Nullable
    public <A extends Annotation> A getWitherAnn(Class<A> annotationClass) {
        return wither == null ? null : annotationCaches.computeIfAbsent(wither, AnnotationCache::new).get(annotationClass);
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

    public Object get(Object target) {
        if (getter == null) return null;
        return ReflectionUtils.invokeMethod(getter, target);
    }

    public void set(Object target, Object value) {
        if (setter == null) return;
        ReflectionUtils.invokeMethod(setter, target, value);
    }

    public Object with(Object target, Object value) {
        if (wither == null) return target;
        return ReflectionUtils.invokeMethod(wither, target, value);
    }
}
