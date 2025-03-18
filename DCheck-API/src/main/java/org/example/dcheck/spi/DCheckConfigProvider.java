package org.example.dcheck.spi;

import lombok.Getter;
import lombok.NonNull;
import org.example.dcheck.api.DCheckConfig;
import org.example.dcheck.support.ConvertMethodDelegateConvertor;
import org.example.dcheck.support.PathConverter;
import org.example.dcheck.support.URLConvertor;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public class DCheckConfigProvider {

    @Getter(lazy = true)
    private static final DCheckConfigProvider instance = new DCheckConfigProvider();

    @Getter
    protected Set<Resource> injectedConfig = Collections.newSetFromMap(new WeakHashMap<>());

    @Getter
    protected ConversionService conversionService;

    @Getter(lazy = true)
    private final DCheckConfig DCheckConfig = new DCheckConfig(Providers.loadConfig("api-config", injectedConfig.toArray(new Resource[0])), conversionService);

    {
        setConversionService(new DefaultConversionService());
    }

    public void setConversionService(@NonNull ConversionService conversionService) {
        this.conversionService = conversionService;
        if (conversionService instanceof ConfigurableConversionService) {
            ConfigurableConversionService service = (ConfigurableConversionService) conversionService;
            service.addConverter(new ConvertMethodDelegateConvertor());
            service.addConverter(new URLConvertor());
            service.addConverter(new PathConverter());
        }
    }
}
