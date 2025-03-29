package org.example.dcheck.util;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Date: 2025/3/29
 * <p></p>
 * Wrapper of {@link Properties}.
 *
 * @author 三石而立Sunsy
 */
public class PropertiesInputStream extends InputStream {

    @Getter
    private final Properties properties;

    private Enumeration<String> propertyKeys;

    private InputStream currentKvStream;

    @Getter
    private String currentKey;

    public PropertiesInputStream(Properties properties) {
        this.properties = properties;
        reset();
    }

    @Override
    public int read() throws IOException {
        while (true) {
            if (currentKvStream == null) {
                if (propertyKeys.hasMoreElements()) {
                    currentKey = propertyKeys.nextElement();
                    currentKvStream = new ByteArrayInputStream(formatProperty(currentKey, properties.getProperty(currentKey)).getBytes(StandardCharsets.UTF_8));
                } else {
                    return -1;
                }
            }
            int read = currentKvStream.read();
            if (read != -1) {
                return read;
            }
            currentKvStream = null;
        }
    }

    @NotNull
    protected String formatProperty(String key, Object value) {
        return key + "=" + value + "\n";
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void reset() {
        propertyKeys = (Enumeration<String>) properties.propertyNames();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof PropertiesInputStream)) return false;

        PropertiesInputStream that = (PropertiesInputStream) object;

        return getProperties().equals(that.getProperties());
    }

    @Override
    public int hashCode() {
        return getProperties().hashCode();
    }

    @Override
    public String toString() {
        return "PropertiesInputStream{" + properties + '}';
    }
}
