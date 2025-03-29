package org.example.dcheck.support;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;

import java.time.Duration;

/**
 * Date: 2025/3/18
 *
 * @author 三石而立Sunsy
 */
public class DurationConverter implements Converter<CharSequence, Duration> {

    private final TypeDescriptor target = new TypeDescriptor(ResolvableType.forClass(Duration.class), null, null);

    @Override
    public Duration convert(@NotNull CharSequence source) {
        try {
            return Duration.parse(source);
        } catch (IllegalArgumentException e) {
            throw new ConversionFailedException(
                    new TypeDescriptor(ResolvableType.forInstance(source), null, null),
                    target,
                    source,
                    e);
        }
    }
}
