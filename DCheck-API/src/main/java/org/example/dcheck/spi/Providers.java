package org.example.dcheck.spi;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.var;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
class Providers {

    private final static ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();


    static <Service> List<Service> findAllImplementations(Class<Service> serviceClass) {
        var loader = ServiceLoader.load(serviceClass);
        var results = new ArrayList<Service>();
        loader.iterator().forEachRemaining(results::add);

        return results.stream().map(AdaptedOrdered::new).sorted(Comparator.comparing(Ordered::getOrder)).map(AdaptedOrdered::getIns).collect(Collectors.toList());
    }

    static <Service> Service findImpl(Class<Service> serviceClass, String specifyKey) {
        var loader = ServiceLoader.load(serviceClass);
        var allImpl = loader.iterator();
        Service candidate = null;
        boolean multiple = false;

        while (allImpl.hasNext()) {
            var cur = allImpl.next();
            if (candidate != null) {
                String implClass = System.getProperty(specifyKey);
                if (implClass != null) {
                    if (implClass.equals(cur.getClass().getCanonicalName())) {
                        return cur;
                    }
                } else {
                    multiple = true;
                }
            }
            candidate = cur;
        }

        if (multiple) {
            var throwImplClass = new ArrayList<String>();
            loader.iterator().forEachRemaining(i -> throwImplClass.add(i.getClass().getCanonicalName()));
            throw new IllegalStateException("multiple '" + serviceClass + "' impl found: please add single implementation on classpath or" +
                    " specify implementation with jvm arg '-D" + specifyKey + "=<impl canonical name>', find implementations: " + throwImplClass);
        }

        if (candidate == null) {
            throw new IllegalStateException("no '" + serviceClass + "' impl found: please add implementation service provider on classpath");
        }
        return candidate;
    }

    static <Ins extends DCheckProvider> Ins instantiate(Class<Ins> insClass) {
        try {
            return insClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    static Properties loadConfig(String configName) {
        try {
            Properties config = new Properties();
            Resource[] resources = resolver.getResources("classpath*:org/example/dcheck/config/" + configName + ".properties");
            for (Resource resource : resources) {
                PropertiesLoaderUtils.fillProperties(config, resource);
            }
            Resource[] localResources = resolver.getResources("file:dcheck-config.properties");
            for (Resource resource : localResources) {
                PropertiesLoaderUtils.fillProperties(config, resource);
            }
            return config;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Getter
    @RequiredArgsConstructor
    static class AdaptedOrdered<Ins> implements Ordered {

        private final Ins ins;

        private final int order;

        public AdaptedOrdered(Ins ins) {
            this.ins = ins;
            order = initOrder();
        }

        private int initOrder() {
            Order order = AnnotationUtils.findAnnotation(ins.getClass(), Order.class);
            if (order != null) {
                if (ins instanceof Ordered) {
                    throw new IllegalStateException("class '" + ins.getClass() + "' is both @Ordered and Ordered");
                }
                return order.value();
            }

            return ins instanceof Ordered ? ((Ordered) ins).getOrder() : Ordered.LOWEST_PRECEDENCE;
        }
    }

}
