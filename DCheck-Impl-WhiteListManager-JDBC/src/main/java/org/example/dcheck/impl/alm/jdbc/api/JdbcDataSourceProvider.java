package org.example.dcheck.impl.alm.jdbc.api;

import org.example.dcheck.impl.alm.jdbc.support.DataSourceInfo;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;

/**
 * Date 2025/04/02
 *
 * @author 三石而立Sunsy
 */
public interface JdbcDataSourceProvider {
    /**
     * 获取与该数据源相关信息
     */
    @NotNull
    DataSourceInfo getDataSourceInfo();

    /**
     * 获取数据源
     */
    @NotNull
    DataSource getDataSource();
}
