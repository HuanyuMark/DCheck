package org.example.dcheck.spi;

import lombok.Getter;
import org.example.dcheck.impl.wlm.jdbc.api.EntityFieldMapper;

import java.util.List;

/**
 * Date: 2025/3/30
 *
 * @author 三石而立Sunsy
 */
public class RuleEntityFieldMapperProvider implements DCheckProvider {
    private static final RuleEntityFieldMapperProvider INSTANCE = new RuleEntityFieldMapperProvider();

    @Getter(lazy = true)
    private final List<EntityFieldMapper> mappers = Providers.findAllImplementations(EntityFieldMapper.class);

    public static RuleEntityFieldMapperProvider getInstance() {
        return INSTANCE;
    }
}
