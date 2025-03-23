package org.example.dcheck.impl.content;

import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.api.ReadableHandler;

import java.io.IOException;
import java.io.Reader;

/**
 * Date: 2025/3/24
 *
 * @author 三石而立Sunsy
 */
@Slf4j
public class ReaderReadableHandler implements ReadableHandler {
    @Override
    public boolean support(Readable readable) {
        return readable instanceof Reader;
    }

    @Override
    public void reset(Readable readable) throws IOException {
        ((Reader) readable).reset();
    }

    @Override
    public String toString(Readable readable) throws IOException {
        reset(readable);
        Reader reader = (Reader) readable;
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[1024]; // 1KB cache
        int charsRead;

        while ((charsRead = reader.read(buffer)) != -1) {
            sb.append(buffer, 0, charsRead);
        }
        try {
            reset(readable);
        } catch (IOException e) {
            log.warn("toString() side effect: reset reader fail: " + e.getMessage(), e);
        }
        return sb.toString();
    }
}
