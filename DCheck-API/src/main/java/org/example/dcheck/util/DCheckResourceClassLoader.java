package org.example.dcheck.util;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * Date 2025/03/18
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public class DCheckResourceClassLoader extends ClassLoader {
    @Getter
    protected static DCheckResourceClassLoader instance = new DCheckResourceClassLoader();

    protected final URLClassLoader target;

    public DCheckResourceClassLoader(URL[] urls, ClassLoader parent) {
        target = new URLClassLoader(urls, parent);
    }

    protected DCheckResourceClassLoader() {
        this(defineUrl(), defineParent());
    }

    protected static URL[] defineUrl() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            return Arrays.stream(resolver.getResources("classpath*:org/example/dcheck/class-resources/")).map(r -> {
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

    protected static ClassLoader defineParent() {
        return DCheckResourceClassLoader.class.getClassLoader();
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return target.loadClass(name);
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
