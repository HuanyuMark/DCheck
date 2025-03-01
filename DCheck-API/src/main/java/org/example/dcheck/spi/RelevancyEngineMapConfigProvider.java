package org.example.dcheck.spi;

import lombok.Getter;

import java.util.Properties;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
public class RelevancyEngineMapConfigProvider implements DCheckProvider {
    @Getter(lazy = true)
    private static final RelevancyEngineMapConfigProvider instance = new RelevancyEngineMapConfigProvider();

    @Getter(lazy = true)
    private final Properties relevancyEngineMap = Providers.loadConfig("relevancy-engine-map");

    protected RelevancyEngineMapConfigProvider() {
    }
}
