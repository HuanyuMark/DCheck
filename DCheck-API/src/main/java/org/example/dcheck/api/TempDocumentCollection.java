package org.example.dcheck.api;

import java.io.Closeable;

/**
 * Date: 2025/2/25
 * the collection will be dropped after the close.
 * use it in try-with-resources statement is the best practice.
 * some spi module would try to avoid resource leak be some special mechanism,
 * but it is not guaranteed. therefore you should remember to close it after using.
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface TempDocumentCollection extends DocumentCollection, Closeable {

    @Override
    default void close() {
        drop();
    }
}
