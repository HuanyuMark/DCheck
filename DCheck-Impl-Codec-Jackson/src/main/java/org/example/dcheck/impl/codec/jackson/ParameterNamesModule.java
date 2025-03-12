package org.example.dcheck.impl.codec.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Date 2025/03/12
 *
 * @author 三石而立Sunsy
 */
public class ParameterNamesModule extends SimpleModule {

    private final JsonCreator.Mode jsonCreatorMode;

    public ParameterNamesModule(JsonCreator.Mode jsonCreatorMode) {
        super("ParameterNamesModule", JacksonCodec.VERSION);
        this.jsonCreatorMode = jsonCreatorMode;
    }

    @Override
    public void setupModule(SetupContext context) {
        context.insertAnnotationIntrospector(new DefaultModeParameterNamesAnnotationIntrospector(jsonCreatorMode));
    }
}
