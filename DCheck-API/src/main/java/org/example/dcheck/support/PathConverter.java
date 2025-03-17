package org.example.dcheck.support;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Date: 2025/3/18
 *
 * @author 三石而立Sunsy
 */
public class PathConverter implements Converter<String, Path> {

    private final TypeDescriptor target = new TypeDescriptor(ResolvableType.forClass(Path.class), null, null);

    @Override
    public Path convert(@NotNull String source) {
        try {
            return Paths.get(source);
        } catch (InvalidPathException e) {
            throw new ConversionFailedException(
                    new TypeDescriptor(ResolvableType.forInstance(source), null, null),
                    target,
                    source,
                    e);
        }
    }
}
