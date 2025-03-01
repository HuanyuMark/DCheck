package org.example.dcheck.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.dcheck.api.TextContent;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@Getter
@RequiredArgsConstructor
public class InMemoryTextContent implements TextContent {
    private final CharSequence text;

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(text.toString().getBytes(StandardCharsets.UTF_8));
    }
}
