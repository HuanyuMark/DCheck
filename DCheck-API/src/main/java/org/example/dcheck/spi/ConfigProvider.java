package org.example.dcheck.spi;

import lombok.Getter;
import org.example.dcheck.api.ApiConfig;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
public class ConfigProvider {
    @Getter(lazy = true)
    private static final ConfigProvider instance = new ConfigProvider();

    @Getter(lazy = true)
    private final ApiConfig apiConfig = new ApiConfig(Providers.loadConfig("api-config"));


}
