package org.example.dcheck.util;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.AbstractResource;

import java.io.InputStream;
import java.util.Properties;

/**
 * Date: 2025/3/29
 * <p>
 * wrapper of {@link Properties}
 *
 * @author 三石而立Sunsy
 */
@Getter
@RequiredArgsConstructor
public class PropertiesResource extends AbstractResource {

    @NonNull
    private final String description;
    @NonNull
    private final Properties properties;

    @Override
    public @NotNull InputStream getInputStream() {
        return new PropertiesInputStream(this.properties);
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof PropertiesResource && this.properties.equals(((PropertiesResource) other).properties);
    }

    @Override
    public int hashCode() {
        return properties.hashCode();
    }
}
