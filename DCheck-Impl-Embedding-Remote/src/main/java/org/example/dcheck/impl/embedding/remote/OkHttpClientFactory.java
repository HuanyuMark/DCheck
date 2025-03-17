package org.example.dcheck.impl.embedding.remote;

import lombok.Getter;
import okhttp3.OkHttpClient;
import org.example.dcheck.api.ApiConfig;
import org.example.dcheck.spi.ConfigProvider;

import java.time.Duration;

import static org.example.dcheck.impl.embedding.remote.ConfigPropertyKey.READ_TIME_OUT;

/**
 * Date: 2025/3/8
 *
 * @author 三石而立Sunsy
 */
public class OkHttpClientFactory {
    @Getter
    private static final OkHttpClientFactory instance = new OkHttpClientFactory();

    public OkHttpClient create() {
        ApiConfig apiConfig = ConfigProvider.getInstance().getApiConfig();
        return new OkHttpClient.Builder()
                .readTimeout(apiConfig.required(READ_TIME_OUT, Duration.class))
                .build();
    }
}
