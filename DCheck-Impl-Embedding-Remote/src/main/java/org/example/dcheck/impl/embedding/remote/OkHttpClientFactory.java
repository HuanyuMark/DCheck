package org.example.dcheck.impl.embedding.remote;

import lombok.Getter;
import okhttp3.OkHttpClient;
import org.example.dcheck.api.DCheckConfig;
import org.example.dcheck.spi.DCheckConfigProvider;

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
        DCheckConfig DCheckConfig = DCheckConfigProvider.getInstance().getDCheckConfig();
        return new OkHttpClient.Builder()
                .readTimeout(DCheckConfig.required(READ_TIME_OUT, Duration.class))
                .build();
    }
}
