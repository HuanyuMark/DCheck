package org.example.sunsy.dcheck.spi;

import lombok.Getter;
import lombok.var;
import org.example.sunsy.dcheck.api.DuplicateChecking;

import java.util.ArrayList;
import java.util.ServiceLoader;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
public class DuplicateCheckingProvider implements DCheckProvider {

    @Getter(lazy = true)
    private final static DuplicateCheckingProvider instance = new DuplicateCheckingProvider();

    @Getter(lazy = true)
    private final DuplicateChecking dCheckImplementation = Providers.findImpl(DuplicateChecking.class,"dcheck.checking.impl");

    protected DuplicateCheckingProvider() {
    }
}
