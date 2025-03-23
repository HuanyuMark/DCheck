package org.example.dcheck.impl.content;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import org.example.dcheck.api.TextContent;
import org.example.dcheck.exception.UnsupportedContentTypeException;
import org.example.dcheck.spi.ReadableHandlerProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;

/**
 * Date: 2025/3/10
 *
 * @author 三石而立Sunsy
 */
@Data
@AllArgsConstructor
public class ReadableTextContent implements TextContent {
    @NonNull
    private Readable reader;

    @Override
    public void resetRead() throws IOException {
        ReadableHandlerProvider.getInstance().reset(reader);
    }

    @Override
    public InputStream getInputStream() {
        return new ReaderInputStream(reader, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        try {
            return ReadableHandlerProvider.getInstance().toString(reader);
        } catch (IOException e) {
            throw new UnsupportedContentTypeException(e);
        }
    }

    protected static class ReaderInputStream extends InputStream {
        private final Readable reader;
        private final CharsetEncoder encoder;
        private final CharBuffer charBuffer;
        private final ByteBuffer byteBuffer = ByteBuffer.allocate(512);
        private boolean endOfInput = false;

        public ReaderInputStream(Readable reader, Charset charset) {
            this.reader = reader;
            this.encoder = charset.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
            this.charBuffer = CharBuffer.allocate(1024);
            charBuffer.flip(); // 初始为读取模式
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            return (read(b, 0, 1) == -1) ? -1 : b[0] & 0xFF;
        }

        @Override
        public int read(byte @NotNull [] b, int off, int len) throws IOException {
            if (!byteBuffer.hasRemaining() && !refillBuffer()) {
                return -1; // 数据已全部读取
            }

            int bytesToCopy = Math.min(len, byteBuffer.remaining());
            byteBuffer.get(b, off, bytesToCopy);
            return bytesToCopy;
        }

        private boolean refillBuffer() throws IOException {
            if (endOfInput) return false;

            byteBuffer.clear(); // 重置字节缓冲区
            CoderResult result;

            do {
                if (!charBuffer.hasRemaining()) { // 需要从Reader读取更多字符
                    charBuffer.clear();
                    int readCount = reader.read(charBuffer);
                    if (readCount == -1) {
                        endOfInput = true;
                        break;
                    }
                    charBuffer.flip(); // 切换为读取模式
                }

                result = encoder.encode(charBuffer, byteBuffer, endOfInput);
                if (result.isOverflow()) {
                    break; // 字节缓冲区已满
                } else if (result.isError()) {
                    result.throwException();
                }
                // 需要更多输入字符
            } while (result.isUnderflow());

            byteBuffer.flip(); // 准备读取字节
            return byteBuffer.hasRemaining() || !endOfInput;
        }

        @Override
        public void close() throws IOException {
            if (reader instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) reader).close();
                } catch (Exception e) {
                    if (e instanceof IOException) throw (IOException) e;
                    throw new IOException(e);
                }
            }
        }
    }
}
