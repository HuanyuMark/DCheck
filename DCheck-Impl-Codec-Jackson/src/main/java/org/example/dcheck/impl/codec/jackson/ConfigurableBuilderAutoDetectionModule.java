package org.example.dcheck.impl.codec.jackson;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.Getter;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Date: 2025/3/13
 *
 * @author 三石而立Sunsy
 */
@Getter
public class ConfigurableBuilderAutoDetectionModule extends SimpleModule {
    private final Set<String> builderDeserializeAutoDetectPackages = new ConcurrentSkipListSet<>();

    public ConfigurableBuilderAutoDetectionModule() {
        super(JacksonCodec.CONFIGURABLE_BUILDER_AUTO_DETECTION_MODULE, JacksonCodec.VERSION);
    }

    @Override
    public void setupModule(SetupContext context) {
        context.insertAnnotationIntrospector(new ConfigurableBuilderAutoDetectIntrospector(
                new JsonPOJOBuilder.Value("build", ""),
                builderDeserializeAutoDetectPackages
        ));
    }
}
