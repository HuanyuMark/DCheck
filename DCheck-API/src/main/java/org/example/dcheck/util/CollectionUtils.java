package org.example.dcheck.util;

import lombok.NonNull;
import org.jetbrains.annotations.Range;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Date: 2025/3/1
 *
 * @author 三石而立Sunsy
 */
public class CollectionUtils {
    public static <E> List<List<E>> partition(@NonNull List<E> list, @Range(from = 2, to = Integer.MAX_VALUE) int chunkSize) {
        if (chunkSize < 2) throw new IllegalArgumentException("chunkSize should be > 1");

        return IntStream.range(0, (list.size() + chunkSize - 1) / chunkSize)
                .mapToObj(i -> list.subList(
                        i * chunkSize,
                        Math.min((i + 1) * chunkSize, list.size()) // 防止越界
                ))
                .collect(Collectors.toList());
    }


    public static <E> Stream<List<E>> partition(@NonNull Stream<E> stream, @Range(from = 2, to = Integer.MAX_VALUE) int chunkSize) {
        if (chunkSize < 2) throw new IllegalArgumentException("chunkSize should be > 1");

        Iterator<E> iterator = stream.iterator();
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<List<E>>(
                Long.MAX_VALUE, Spliterator.ORDERED) {

            private List<E> list = new ArrayList<>();

            @Override
            public boolean tryAdvance(Consumer<? super List<E>> action) {
                if (!iterator.hasNext()) {
                    if (!list.isEmpty()) {
                        action.accept(list);
                        list = Collections.emptyList();
                    }
                    return false;
                }

                list.add(iterator.next());
                if (list.size() == chunkSize) {
                    action.accept(list);
                    list = new ArrayList<>();
                }

                return true;
            }
        }, false);
    }
}
