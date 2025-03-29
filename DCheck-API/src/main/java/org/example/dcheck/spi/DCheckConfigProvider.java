package org.example.dcheck.spi;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.api.DCheckConfig;
import org.example.dcheck.api.DuplicateChecking;
import org.example.dcheck.support.ConvertMethodDelegateConvertor;
import org.example.dcheck.support.PathConverter;
import org.example.dcheck.support.URLConvertor;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;

import java.util.HashSet;
import java.util.Set;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@Slf4j
public class DCheckConfigProvider {

    @Getter(lazy = true)
    private static final DCheckConfigProvider instance = new DCheckConfigProvider();

    /**
     * add customized config.
     * override default config and inherit default config-loading mechanism
     * <p>
     * inject {@link java.util.Properties}:
     * <p>
     * {@code new PropertiesResource("the is my programing config", myConfig)}
     * <p>
     * Note:  call {@code getInjectedConfigResources().clear()} after
     * {@link DuplicateChecking#init()} to release resources
     * or enable {@link DCheckConfig#DCHECK_CONFIG_AUTO_CLEAR} (default is enabled)
     * to avoid memory leak
     *
     * @see org.example.dcheck.util.PropertiesResource PropertiesResource
     */
    @Getter
    protected Set<Resource> injectedConfigResources = new HashSet<>();

    @Getter
    protected ConversionService conversionService;

    private volatile DCheckConfig config;

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

    /**
     * get or lazy load config instance. all configs would be applied, see associated config below.
     * you should customize {@link DCheckConfigProvider} before call that method.
     *
     * @see #getInjectedConfigResources()
     * @see #getConversionService()
     * @see #setConversionService(ConversionService)
     */
    public DCheckConfig getDCheckConfig() {
        if (config == null) {
            synchronized (this) {
                if (config == null) {
                    config = new DCheckConfig(Providers.loadConfig("api-config", injectedConfigResources.toArray(new Resource[0])), conversionService);
                }
            }
        }
        return config;
    }
}
