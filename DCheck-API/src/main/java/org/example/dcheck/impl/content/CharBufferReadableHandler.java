package org.example.dcheck.impl.content;

import org.example.dcheck.api.ReadableHandler;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * Date: 2025/3/24
 *
 * @author 三石而立Sunsy
 */
public class CharBufferReadableHandler implements ReadableHandler {
    @Override
    public boolean support(Readable readable) {
        return readable instanceof CharBuffer;
    }

    @Override
    public void reset(Readable readable) throws IOException {
        ((CharBuffer) readable).reset();
    }

    @Override
    public String toString(Readable readable) throws IOException {
        reset(readable);
        return ((CharBuffer) readable).toString();
    }
}
