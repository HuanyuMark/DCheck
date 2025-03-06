package org.example.dcheck.common.util;

import lombok.Data;
import lombok.Getter;
import lombok.var;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Date: 2025/2/27
 *
 * @author 三石而立Sunsy
 */
@Data
public class I18nText {
    /**
     * file name without ext
     */
    private final String name;
    private final String extName;
    private final Locale locale;

    @Nullable
    @Getter(lazy = true)
    private final String content = loadContent();

    public I18nText(String name, String extName, Locale locale) {
        this.name = name;
        this.extName = extName;
        this.locale = locale;
        InputStream in = getInputStream();
        if (in == null) {
            throw new IllegalArgumentException("cannot find i18n text file, expected resource is '" + getResourceKey() + "': " + this);
        }
        try {
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String loadContent() {
        try (var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getInputStream()), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public InputStream getInputStream() {
        return getClass().getClassLoader().getResourceAsStream(getResourceKey());
    }

    @NotNull
    protected String getResourceKey() {
        return "/i18n/" + name + "." + extName;
    }
}
