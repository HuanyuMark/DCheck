package org.example.dcheck.impl.codec.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.*;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Executable;
import java.lang.reflect.MalformedParametersException;
import java.lang.reflect.Parameter;
import java.util.function.Function;

/**
 * Date 2025/03/12
 *
 * @author 三石而立Sunsy
 */
public class DefaultModeParameterNamesAnnotationIntrospector extends NopAnnotationIntrospector {
    private static final long serialVersionUID = 1L;

    private final JsonCreator.Mode creatorBinding;

    @Getter
    @Setter
    private Function<Executable, Parameter[]> parameterExtractor = Executable::getParameters;

    DefaultModeParameterNamesAnnotationIntrospector(JsonCreator.Mode creatorBinding) {
        this.creatorBinding = creatorBinding;
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

        Parameter p = params[annotatedParameter.getIndex()];
        return p.isNamePresent() ? p.getName() : null;
    }

    private Parameter[] getParameters(AnnotatedWithParams owner) {
        if (owner instanceof AnnotatedConstructor) {
            return parameterExtractor.apply(((AnnotatedConstructor) owner).getAnnotated());
        }
        if (owner instanceof AnnotatedMethod) {
            return parameterExtractor.apply(((AnnotatedMethod) owner).getAnnotated());
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
        if (ann != null) {
            JsonCreator.Mode mode = ann.mode();
            // but keep in mind that there may be explicit default for this module
            if ((creatorBinding != null)
                    && (mode == JsonCreator.Mode.DEFAULT)) {
                mode = creatorBinding;
            }
            return mode;
        }
        return creatorBinding;
    }
}
