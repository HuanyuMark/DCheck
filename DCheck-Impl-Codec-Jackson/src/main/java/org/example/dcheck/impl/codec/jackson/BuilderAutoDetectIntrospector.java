package org.example.dcheck.impl.codec.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Date 2025/03/12
 *
 * @author 三石而立Sunsy
 */
@Slf4j
@RequiredArgsConstructor
public class BuilderAutoDetectIntrospector extends AnnotationIntrospector {
    @SuppressWarnings("all")
    protected static final Class[] emptyClasses = new Class[0];
    private final JsonPOJOBuilder.Value builderConfig;

    @Getter
    @Setter
    @NonNull
    private String builderGenerateMethodName = "builder";

    /**
     * if the placement of builder class definition is outer of the built class(pojo):<br>
     * true: use it as builder class
     * false: use it only if it is the inner class of the built class
     */
    @Getter
    @Setter
    private boolean useOuterBuilderClass;

    @Override
    public Version version() {
        return JacksonCodec.VERSION;
    }

    @SuppressWarnings("unchecked")
    protected <A extends Annotation> A _findAnnotation(Annotated ann,
                                                       Class<A> annoClass) {
        if (!JsonSerialize.class.isAssignableFrom(annoClass)) return null;
        if (!(ann instanceof AnnotatedClass)) return null;
        AnnotatedClass ac = (AnnotatedClass) ann;
        if (!supportDetect(ac)) {
            return null;
        }
        Class<?> pojo = ac.getRawType();
        JsonSerialize jsonSerialize = AnnotationUtils.findAnnotation(pojo, JsonSerialize.class);
        if (jsonSerialize != null) return (A) jsonSerialize;
        var builderType = findBuilder(ac);
        if (builderType == null) return null;

        var attributes = new AnnotationAttributes(2);
        attributes.put("builder", builderType);
        return (A) AnnotationUtils.synthesizeAnnotation(attributes, JsonSerialize.class, pojo);
    }

    @Nullable
    protected Class<?> findBuilder(AnnotatedClass ac) {
        Class<?> builderClass = ac.getRawType();

        Method builderMethod;
        try {
            builderMethod = builderClass.getDeclaredMethod(builderGenerateMethodName);
            if (!Modifier.isStatic(builderMethod.getModifiers())) {
                log.warn("builder method must be static: {}", builderMethod);
                return null;
            }
        } catch (NoSuchMethodException e) {
            return null;
        }

        var candidateBuilderType = builderMethod.getReturnType();
        Class<?>[] innerTypes;
        try {
            innerTypes = builderClass.getDeclaredClasses();
        } catch (SecurityException e) {
            log.warn("fail to get inner types of class: {}", builderClass.getName(), e);
            return null;
        }

        return Arrays.stream(innerTypes).filter(candidateBuilderType::isAssignableFrom)
                .findFirst()
                .orElseGet(() -> {
                    if (useOuterBuilderClass && !Modifier.isAbstract(candidateBuilderType.getModifiers()) && !candidateBuilderType.isInterface()) {
                        return candidateBuilderType;
                    }
                    return null;
                });
    }

    @Override
    public Class<?> findPOJOBuilder(AnnotatedClass ac) {
        if (!supportDetect(ac)) {
            // apply default value
            return null;
        }
        return findBuilder(ac);
    }

    @Override
    public JsonPOJOBuilder.Value findPOJOBuilderConfig(AnnotatedClass ac) {
        JsonPOJOBuilder annotation = ac.getAnnotation(JsonPOJOBuilder.class);
        if (annotation == null) {
            if (supportDetect(ac)) return builderConfig;
            return null;
        }
        return new JsonPOJOBuilder.Value(annotation);
    }

    protected boolean supportDetect(AnnotatedClass ac) {
        return ac.getRawType().getName().startsWith("org.example.dcheck") && !ac.hasAnnotation(JsonPOJOBuilder.class);
    }
}
