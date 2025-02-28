package org.example.dcheck.common.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Date: 2025/3/1
 *
 * @author 三石而立Sunsy
 */
public class CollectionUtils {
    public static <E> List<List<E>> partition(List<E> list, int chunkSize) {
        return IntStream.range(0, (list.size() + chunkSize - 1) / chunkSize)
                .mapToObj(i -> list.subList(
                        i * chunkSize,
                        Math.min((i + 1) * chunkSize, list.size()) // 防止越界
                ))
                .collect(Collectors.toList());
    }


}
