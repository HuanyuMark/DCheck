package org.example.dcheck.support;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Date: 2025/3/17
 *
 * @author 三石而立Sunsy
 */
public class URLConvertor implements Converter<String, URL> {

    private final TypeDescriptor target = new TypeDescriptor(ResolvableType.forClass(URL.class), null, null);

    @Override
    public URL convert(@NotNull String source) {
        try {
            return URI.create(source).toURL();
        } catch (IllegalArgumentException | MalformedURLException e) {
            throw new ConversionFailedException(
                    new TypeDescriptor(ResolvableType.forInstance(source), null, null),
                    target,
                    source,
                    e);
        }
    }
}
