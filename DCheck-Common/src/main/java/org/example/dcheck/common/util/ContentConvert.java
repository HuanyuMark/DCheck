package org.example.dcheck.common.util;

import lombok.var;
import org.example.dcheck.api.Content;
import org.example.dcheck.api.TextContent;
import org.example.dcheck.impl.InMemoryTextContent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Date: 2025/3/1
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public class ContentConvert {

    public static String castToText(Content content) {
        //TODO support other content type
        try {
            if (content instanceof InMemoryTextContent) return ((InMemoryTextContent) content).getText().toString();
            if (content instanceof TextContent) return readStreamAsString(content.getInputStream());
            throw new UnsupportedOperationException();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Content castToContent(Object input) {
        if (input instanceof CharSequence) {
            return new InMemoryTextContent((CharSequence) input);
        }
        //TODO support other types
        throw new UnsupportedOperationException();
    }

    public static String readStreamAsString(InputStream inputStream) throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining());
        }
    }
}
