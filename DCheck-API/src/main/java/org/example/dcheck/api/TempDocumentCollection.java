package org.example.dcheck.api;

import java.io.Closeable;

/**
 * Date: 2025/2/25
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface TempDocumentCollection extends DocumentCollection, Closeable {

    @Override
    default void close() {
        drop();
    }
}
