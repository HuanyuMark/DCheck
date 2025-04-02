package org.example.dcheck.impl.alm.jdbc.support;

import lombok.Data;
import lombok.NonNull;

/**
 * Date 2025/04/02
 * associated info for a {@link javax.sql.DataSource}
 *
 * @author 三石而立Sunsy
 */
public interface DataSourceInfo {

    @NonNull
    String getDatabaseType();

    @Data
    class Default implements DataSourceInfo {
        @NonNull
        final String databaseType;
    }
}
