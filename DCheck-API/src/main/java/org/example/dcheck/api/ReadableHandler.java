package org.example.dcheck.api;

import org.example.dcheck.impl.content.ReadableTextContent;

import java.io.IOException;

/**
 * Date: 2025/3/24
 * spi to support {@link ReadableTextContent} reset
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface ReadableHandler {
    /**
     * make sure call that method before call any method declare in this interface
     */
    boolean support(Readable readable);

    void reset(Readable readable) throws IOException;

    /**
     * @implNote invoke {@link #reset(Readable)} before and after that method invocation
     */
    String toString(Readable readable) throws IOException;
}
