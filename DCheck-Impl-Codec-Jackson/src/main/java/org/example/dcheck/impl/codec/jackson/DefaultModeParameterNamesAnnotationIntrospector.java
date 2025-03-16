package org.example.dcheck.impl.codec.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.annotation.AnnotationUtils;

import java.beans.ConstructorProperties;
import java.lang.reflect.Executable;
import java.lang.reflect.MalformedParametersException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Date 2025/03/12
 *
 * @author 三石而立Sunsy
 */
@Slf4j
public class DefaultModeParameterNamesAnnotationIntrospector extends AnnotationIntrospector {
    private static final long serialVersionUID = 1L;

    private final JsonCreator.Mode creatorBinding;

    @Getter
    @Setter
    private Function<Executable, Parameter[]> parameterExtractor = Executable::getParameters;

    DefaultModeParameterNamesAnnotationIntrospector(JsonCreator.Mode creatorBinding) {
        this.creatorBinding = creatorBinding;
    }

    @Override
    public Version version() {
        return JacksonCodec.VERSION;
    }

    @Override
    public String findImplicitPropertyName(AnnotatedMember m) {
        if (m instanceof AnnotatedParameter) {
            return findParameterName((AnnotatedParameter) m);
        }
        return null;
    }

    private String findParameterName(AnnotatedParameter annotatedParameter) {

        Parameter[] params;
        try {
            params = getParameters(annotatedParameter.getOwner());
        } catch (MalformedParametersException e) {
            return null;
        }

        if (params == null) return null;

        Parameter p = params[annotatedParameter.getIndex()];
        return p.isNamePresent() ? p.getName() : null;
    }

    private Parameter[] getParameters(AnnotatedWithParams owner) {
        if (owner.getAnnotated() instanceof Executable) {
            return parameterExtractor.apply((Executable) owner.getAnnotated());
        }
        return null;
    }

/*
/**********************************************************
/* Creator information handling
/**********************************************************
 */

    @Override
    public JsonCreator.Mode findCreatorAnnotation(MapperConfig<?> config, Annotated a) {
        JsonCreator ann = _findAnnotation(a, JsonCreator.class);
        if (ann == null) {
            return injectMode(config, a);
        }
        JsonCreator.Mode mode = ann.mode();
        // but keep in mind that there may be explicit default for this module
        if ((creatorBinding != null)
                && (mode == JsonCreator.Mode.DEFAULT)) {
            mode = creatorBinding;
        }
        return mode;
    }

    @Nullable
    protected JsonCreator.Mode injectMode(MapperConfig<?> config, Annotated a) {
        if (a.getRawType().getDeclaredConstructors().length == 1) {
            return creatorBinding;
        }

        if (Arrays.stream(a.getRawType().getDeclaredConstructors()).anyMatch(c -> AnnotationUtils.findAnnotation(c, ConstructorProperties.class) != null)) {
            return null;
        }

        AnnotatedClass ac = config.introspectClassAnnotations(a.getType()).getClassInfo();

        if (config.getAnnotationIntrospector().allIntrospectors().stream().anyMatch(intro -> intro.findPOJOBuilder(ac) != null)) {
            return null;
        }

        log.warn("multiple constructor found. fail to bind single constructor as JsonCreator: " + a);
        return null;
    }
}
