package org.example.dcheck.spi;

import lombok.Getter;
import org.example.dcheck.api.ReadableHandler;

import java.io.IOException;
import java.util.List;

/**
 * Date: 2025/3/24
 *
 * @author 三石而立Sunsy
 */
public class ReadableHandlerProvider implements DCheckProvider, ReadableHandler {

    @Getter
    private static final ReadableHandlerProvider instance = new ReadableHandlerProvider();

    @Getter(lazy = true)
    private final List<ReadableHandler> handlers = Providers.findAllImplementations(ReadableHandler.class);

    @Override
    public boolean support(Readable readable) {
        return getHandlers().stream().anyMatch(handler -> handler.support(readable));
    }

    @Override
    public void reset(Readable readable) throws IOException {
        match(readable).reset(readable);
    }

    protected ReadableHandler match(Readable readable) throws IOException {
        return getHandlers().stream().filter(handler -> handler.support(readable))
                .findFirst().orElseThrow(() -> new IOException("Unsupported Reset That Readable: " + readable));
    }

    @Override
    public String toString(Readable readable) throws IOException {
        return match(readable).toString(readable);
    }
}
