package org.example.dcheck.spi;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.api.ParagraphRelevancyEngine;
import org.example.dcheck.util.UtilConst;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@Slf4j
class Providers {
    static final String AGGREGATE_CONFIG_NAME = "dcheck-config.properties";

    /**
     * load the newest properties...
     */
    static Properties loadBaseProperties() {
        return System.getenv().entrySet().stream().collect(() -> new Properties(System.getProperties()), (p, e) -> p.put(e.getKey(), e.getValue()), Hashtable::putAll);
    }

    /**
     * load all impl at startup. maybe lead to a performance problem.
     * define an init() method in these impls, and call init() method in your code is
     * recommended.
     *
     * @see ParagraphRelevancyEngine#init()  ParagraphRelevancyEngine.init()
     */
    static <Service> List<Service> findAllImplementations(Class<Service> serviceClass) {
        ServiceLoader<Service> loader = ServiceLoader.load(serviceClass);
        List<Service> results = new ArrayList<>();
        try {
            loader.iterator().forEachRemaining(results::add);
        } catch (Throwable e) {
            throw new IllegalStateException("Service Instantiate Fail: " + e.getCause().getMessage(), e.getCause());
        }

        return results.stream().map(AdaptedOrdered::new).sorted(Comparator.comparing(Ordered::getOrder)).map(AdaptedOrdered::getIns).collect(Collectors.toList());
    }

    @SuppressWarnings("all")
    static <Service> Service findImpl(Class<Service> serviceClass, String specifyKey) {
        ServiceLoader<Service> loader = ServiceLoader.load(serviceClass);
        Iterator<Service> allImpl = loader.iterator();
        Service candidate = null;
        boolean multiple = false;

        while (allImpl.hasNext()) {
            Service cur = allImpl.next();
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
            List<String> throwImplClass = new ArrayList<>();
            loader.iterator().forEachRemaining(i -> throwImplClass.add(i.getClass().getCanonicalName()));
            throw new IllegalStateException("multiple '" + serviceClass + "' impl found: please add single implementation on classpath or" +
                    " specify implementation with jvm arg '-D" + specifyKey + "=<impl canonical name>', find implementations: " + throwImplClass);
        }

        if (candidate == null) {
            throw new IllegalStateException("no '" + serviceClass + "' impl found: please add implementation service provider on classpath");
        }
        return candidate;
    }

    static <Ins> Ins instantiate(Class<Ins> insClass) {
        try {
            return insClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static <Service> Service createService(Properties map, String instanceName, String mapKey) {
        String className = map.getProperty(mapKey);
        if (className == null) {
            throw new IllegalArgumentException("unsupported " + instanceName + ": '" + mapKey + "'");
        }
        try {
            return instantiate((Class<Service>) Class.forName(className));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("create service fail: " + e.getMessage(), e);
        }
    }

    static Properties loadConfig(String configName, Resource... injects) {
        try {
            Properties base = new Properties(loadBaseProperties());
            Properties config = new Properties(base);

            // 读取类路径中最匹配的配置
            Resource[] resources = UtilConst.RESOLVER.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "org/example/dcheck/config/" + configName + ".properties");
            for (Resource resource : resources) {
                PropertiesLoaderUtils.fillProperties(config, resource);
            }

            if (resources.length == 0) {
                log.warn("no config found in classpath, please add config file to classpath or jar file: org/example/dcheck/config/{}.properties", configName);
            }

            // 读取在jar包中的 aggregate 配置
            for (Resource resource : UtilConst.RESOLVER.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "org/example/dcheck/config/" + AGGREGATE_CONFIG_NAME)) {
                PropertiesLoaderUtils.fillProperties(config, resource);
            }

            // 读取在工作目录下的 aggregate 配置
            if (!Files.exists(Paths.get(AGGREGATE_CONFIG_NAME))) return config;

            Resource[] localResources = UtilConst.RESOLVER.getResources(ResourceUtils.FILE_URL_PREFIX + AGGREGATE_CONFIG_NAME);
            for (Resource resource : localResources) {
                PropertiesLoaderUtils.fillProperties(config, resource);
            }
            for (Resource inject : injects) {
                PropertiesLoaderUtils.fillProperties(config, inject);
            }

            if (injects.length != 0) {
                log.info("load config '{}' with injected resources: {}", configName, Arrays.asList(injects));
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
