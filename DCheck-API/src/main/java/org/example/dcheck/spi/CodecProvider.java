package org.example.dcheck.spi;

import lombok.Getter;
import org.example.dcheck.api.Codec;

import java.util.List;

/**
 * Date: 2025/3/1
 *
 * @author 三石而立Sunsy
 */
public class CodecProvider implements DCheckProvider {
    @Getter
    private static final CodecProvider instance = new CodecProvider();

    public List<Codec> getCodecs() {
        return Providers.findAllImplementations(Codec.class);
    }
}
