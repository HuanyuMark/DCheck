package org.example.dcheck.spi;

import lombok.Getter;
import org.example.dcheck.api.DuplicateChecking;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
public class DuplicateCheckingProvider implements DCheckProvider {

    @Getter(lazy = true)
    private final static DuplicateCheckingProvider instance = new DuplicateCheckingProvider();

    @Getter(lazy = true)
    private final DuplicateChecking impl = Providers.findImpl(DuplicateChecking.class, "dcheck.checking.impl");

    protected DuplicateCheckingProvider() {
    }
}
