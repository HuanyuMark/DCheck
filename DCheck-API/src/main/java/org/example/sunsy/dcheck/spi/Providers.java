package org.example.sunsy.dcheck.spi;

import lombok.var;
import org.example.sunsy.dcheck.api.DuplicateChecking;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.ServiceLoader;

/**
 * Date 2025/02/26
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
class Providers {
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
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
