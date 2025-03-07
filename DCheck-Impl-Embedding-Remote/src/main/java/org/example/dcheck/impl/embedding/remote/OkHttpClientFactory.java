package org.example.dcheck.impl.embedding.remote;

import lombok.Getter;
import lombok.var;
import okhttp3.OkHttpClient;
import org.example.dcheck.spi.ConfigProvider;

import java.time.Duration;
import java.time.format.DateTimeParseException;

/**
 * Date: 2025/3/8
 *
 * @author 三石而立Sunsy
 */
public class OkHttpClientFactory {
    public static final String READ_TIME_OUT = "relevancy-engine.model.embedding.remote.timeout";
    @Getter
    private static final OkHttpClientFactory instance = new OkHttpClientFactory();

    public OkHttpClient create() {
        var apiConfig = ConfigProvider.getInstance().getApiConfig();
        String timeout = apiConfig.getProperty(READ_TIME_OUT);
        Duration timeoutDuration;
        try {
            timeoutDuration = Duration.parse(timeout);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("invalid config '" + READ_TIME_OUT + "=" + timeout + "': " + e.getMessage(), e);
        }
        return new OkHttpClient.Builder()
                .readTimeout(timeoutDuration)
                .build();
    }
}
