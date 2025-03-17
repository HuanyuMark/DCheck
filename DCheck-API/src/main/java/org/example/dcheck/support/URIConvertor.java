package org.example.dcheck.support;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;

import java.net.URI;

/**
 * Date: 2025/3/17
 *
 * @author 三石而立Sunsy
 */
public class URIConvertor implements Converter<String, URI> {

    private final TypeDescriptor target = new TypeDescriptor(ResolvableType.forClass(URI.class), null, null);

    @Override
    public URI convert(@NotNull String source) {
        try {
            return URI.create(source);
        } catch (IllegalArgumentException e) {
            throw new ConversionFailedException(
                    new TypeDescriptor(ResolvableType.forInstance(source), null, null),
                    target,
                    source,
                    e);
        }
    }
}
