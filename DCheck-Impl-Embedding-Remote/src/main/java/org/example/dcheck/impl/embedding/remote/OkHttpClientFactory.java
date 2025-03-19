package org.example.dcheck.impl.embedding.remote;

import lombok.Getter;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.example.dcheck.api.DCheckConfig;
import org.example.dcheck.spi.DCheckConfigProvider;
import org.example.dcheck.util.DCheckExecutorService;

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
        DCheckConfig config = DCheckConfigProvider.getInstance().getDCheckConfig();
        Dispatcher dispatcher = new Dispatcher(new DCheckExecutorService());
        dispatcher.setMaxRequestsPerHost(config.requiredPositiveInt(ConfigPropertyKey.EMBEDDING_REMOTE_PARALLELISM));
        return new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .readTimeout(config.required(READ_TIME_OUT, Duration.class))
                .build();
    }
}
