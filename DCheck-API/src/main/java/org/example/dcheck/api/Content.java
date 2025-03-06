package org.example.dcheck.api;

import java.io.InputStream;

/**
 * Date 2025/02/26
 * 用来替代 InputStream 读取的类，以加快对于特定数据读取的速度
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface Content {
    InputStream getInputStream();

    default void resetRead() {
    }
}
