package org.example.dcheck.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.stream.Stream;

/**
 * Date 2025/03/18
 *
 * @author 三石而立Sunsy
 * @apiNote load class dynamic is an operation needs to be carefully considered
 */
@Slf4j
public class DCheckResourceClassLoader extends ClassLoader implements Closeable {

    public static final String UNPACK_LOCATION_PATTERN = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "org/example/dcheck/class-resources/";
    public static final String JAR_LOCATION_PATTERN = UNPACK_LOCATION_PATTERN + "**/*.jar";
    @Getter
    protected static DCheckResourceClassLoader shared = new DCheckResourceClassLoader();

    protected final URLClassLoader target;

    public DCheckResourceClassLoader(URL[] urls, ClassLoader parent) {
        target = new URLClassLoader(urls, parent);
    }

    public DCheckResourceClassLoader(ClassLoader parent) {
        target = new URLClassLoader(defineUrl(), parent);
    }

    protected DCheckResourceClassLoader() {
        target = new URLClassLoader(defineUrl(), defineParent());

        log.info("load urls: {}", Arrays.asList(target.getURLs()));
    }

    protected URL[] defineUrl() {
        try {
            return Stream.concat(Arrays.stream(UtilConst.RESOLVER.getResources(UNPACK_LOCATION_PATTERN)), Arrays.stream(UtilConst.RESOLVER.getResources(JAR_LOCATION_PATTERN))).map(r -> {
                try {
                    return r.getURL();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).toArray(URL[]::new);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected ClassLoader defineParent() {
        return DCheckResourceClassLoader.class.getClassLoader();
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> clazz = target.loadClass(name);
        log.info("load class: {}", clazz);
        return clazz;
    }


    @Override
    public @Nullable URL getResource(String name) {
        return target.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return target.getResources(name);
    }

    @Override
    public void setDefaultAssertionStatus(boolean enabled) {
        target.setDefaultAssertionStatus(enabled);
    }

    @Override
    public void setPackageAssertionStatus(String packageName, boolean enabled) {
        target.setPackageAssertionStatus(packageName, enabled);
    }

    @Override
    public void setClassAssertionStatus(String className, boolean enabled) {
        target.setClassAssertionStatus(className, enabled);
    }

    @Override
    public void clearAssertionStatus() {
        target.clearAssertionStatus();
    }

    @Override
    public @Nullable InputStream getResourceAsStream(String name) {
        return target.getResourceAsStream(name);
    }

    @Override
    public void close() throws IOException {
        target.close();
    }

    public URL[] getURLs() {
        return target.getURLs();
    }

    @Override
    public URL findResource(String name) {
        return target.findResource(name);
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        return target.findResources(name);
    }
}
