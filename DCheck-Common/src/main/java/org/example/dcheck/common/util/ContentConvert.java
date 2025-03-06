package org.example.dcheck.common.util;

import org.example.dcheck.api.Content;
import org.example.dcheck.api.TextContent;
import org.example.dcheck.impl.InMemoryTextContent;

import java.io.*;
import java.nio.charset.StandardCharsets;

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
        final int bufferSize = 1024;
        InputStream bis = inputStream instanceof ByteArrayInputStream || inputStream instanceof BufferedInputStream ? inputStream : new BufferedInputStream(inputStream);
        ByteArrayOutputStream result = new ByteArrayOutputStream();

        byte[] buffer = new byte[bufferSize];
        int bytesRead;
        while ((bytesRead = bis.read(buffer, 0, bufferSize)) != -1) {
            result.write(buffer, 0, bytesRead);
        }

        return result.toString(StandardCharsets.UTF_8.name());
    }
}
