package org.example.dcheck.util;

import lombok.Getter;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Date: 2025/4/6
 * Get annotation from {@link AnnotatedElement} and cached that result.
 * Dependent on {@link AnnotationUtils}
 *
 * @author 三石而立Sunsy
 * @see AnnotationUtils
 * @see org.springframework.core.annotation.MergedAnnotations
 */
public abstract class AnnotationCache {
    @Getter
    protected final AnnotatedElement annotatedElement;
    protected final Map<Class<? extends Annotation>, Annotation> cache = new ConcurrentHashMap<>();

    protected AnnotationCache(AnnotatedElement annotatedElement) {
        this.annotatedElement = annotatedElement;
    }

    public static AnnotationCache of(AnnotatedElement el) {
        if (el instanceof Class<?>) {
            return new AnnotationCache(el) {
                @Override
                protected Annotation find(Class<? extends Annotation> ann) {
                    return AnnotationUtils.findAnnotation(annotatedElement, ann);
                }
            };
        }
        if (el instanceof Method) {
            return new AnnotationCache(el) {
                @Override
                protected Annotation find(Class<? extends Annotation> ann) {
                    return AnnotationUtils.findAnnotation((Method) annotatedElement, ann);
                }
            };
        }
        return new AnnotationCache(el) {
            @Override
            protected Annotation find(Class<? extends Annotation> ann) {
                return AnnotationUtils.findAnnotation(annotatedElement, ann);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public <A extends Annotation> A get(Class<A> annotationClass) {
        return ((A) cache.computeIfAbsent(annotationClass, this::find));
    }

    protected abstract Annotation find(Class<? extends Annotation> ann);
}
