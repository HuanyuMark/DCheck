package org.example.dcheck.spi;

import lombok.Getter;
import lombok.var;
import org.example.dcheck.api.ParagraphRelevancyEngine;
import org.example.dcheck.impl.EmbeddingFunction;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
public class MapSpi implements DCheckProvider {
    @Getter(lazy = true)
    private static final MapSpi instance = new MapSpi();

    public EmbeddingFunction getFunc(String modelKey) {
        return createService(MapConfigProvider.getInstance().getEmbeddingFuncMap(), "embedding model", modelKey);
    }

    public ParagraphRelevancyEngine getRelevancyEngine(String relevancyEngineKey) {
        return createService(MapConfigProvider.getInstance().getRelevancyEngineMap(), "relevancy engine", relevancyEngineKey);
    }

    @SuppressWarnings("unchecked")
    protected <Service> Service createService(Properties map, String instanceName, String mapKey) {
        var classname = MapConfigProvider.getInstance().getRelevancyEngineMap().getProperty(mapKey);
        if (classname == null) {
            throw new IllegalArgumentException("unsupported " + instanceName + ": '" + mapKey + "'");
        }
        try {
            return (Service) Class.forName(classname).getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                 ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
