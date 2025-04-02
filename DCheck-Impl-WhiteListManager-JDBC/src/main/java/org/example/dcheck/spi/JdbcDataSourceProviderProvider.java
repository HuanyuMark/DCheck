package org.example.dcheck.spi;

import lombok.Getter;
import org.example.dcheck.impl.alm.jdbc.api.JdbcDataSourceProvider;

/**
 * Date 2025/04/02
 *
 * @author 三石而立Sunsy
 */
public class JdbcDataSourceProviderProvider {
    protected static final JdbcDataSourceProviderProvider INSTANCE = new JdbcDataSourceProviderProvider();

    public static JdbcDataSourceProviderProvider getInstance() {
        return INSTANCE;
    }

    protected JdbcDataSourceProviderProvider() {
    }

    @Getter(lazy = true)
    final JdbcDataSourceProvider provider = Providers.findAllImplementations(JdbcDataSourceProvider.class).stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("No JdbcDataSourceProvider Found: please add JdbcDataSourceProvider implementation by SPI"));
}
