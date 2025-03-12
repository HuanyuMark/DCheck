package org.example.dcheck.impl.codec.jackson;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;

import java.util.Collection;

/**
 * Date: 2025/3/13
 *
 * @author 三石而立Sunsy
 */
public class ConfigurableBuilderAutoDetectIntrospector extends BuilderAutoDetectIntrospector {

    private final Collection<String> builderDeserializeAutoDetectPackages;

    public ConfigurableBuilderAutoDetectIntrospector(JsonPOJOBuilder.Value builderConfig, Collection<String> builderDeserializeAutoDetectPackages) {
        super(builderConfig);
        this.builderDeserializeAutoDetectPackages = builderDeserializeAutoDetectPackages;
    }

    @Override
    protected boolean supportDetect(AnnotatedClass ac) {
        return super.supportDetect(ac) || builderDeserializeAutoDetectPackages.stream().anyMatch(p -> ac.getRawType().getName().startsWith(p));
    }
}
