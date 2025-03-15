package org.example.dcheck.support;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.example.dcheck.api.PreloadClass;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Date 2025/03/12
 * preload class to invoke static initialize methods
 *
 * @author 三石而立Sunsy
 */
@Slf4j
@Builder
@NoArgsConstructor
public class PreloadClassLoader {
    @Singular
    private final List<ClassLoader> classLoaders = new ArrayList<>(Collections.singleton(getClass().getClassLoader()));
    @Singular
    private final List<String> packages = new ArrayList<>(Collections.singleton("org.example.dcheck"));

    public void perform() {
        Reflections reflections = buildReflections();
        try {
            Set<Class<? extends PreloadClass>> preloaded = reflections.getSubTypesOf(PreloadClass.class);
            preloaded.forEach(this::doPreload);
            log.info("preload PreloadClass success: {}", preloaded);
        } catch (Throwable e) {
            throw new IllegalStateException("preload fail: " + e.getMessage(), e);
        }
    }

    protected void doPreload(Class<? extends PreloadClass> clazz) {
        ReflectionUtils.doWithLocalMethods(clazz, method -> {
            if (AnnotationUtils.findAnnotation(method, PreloadClass.PreloadMethod.class) == null) {
                return;
            }
            if (method.getParameterCount() != 0 || !Modifier.isStatic(method.getModifiers())) {
                throw new IllegalArgumentException("@PreloadClass.PreloadMethod method must be static and no parameters: " + method);
            }
            ReflectionUtils.makeAccessible(method);
            ReflectionUtils.invokeMethod(method, null);
        });
    }

    @NotNull
    protected Reflections buildReflections() {
        ClassLoader[] classLoaders = this.classLoaders.toArray(new ClassLoader[0]);
        ConfigurationBuilder builder = new ConfigurationBuilder();
        for (String pkg : packages) {
            builder.addUrls(ClasspathHelper.forPackage(pkg, classLoaders));
        }
        builder.addScanners(Scanners.SubTypes);
        builder.addClassLoaders(classLoaders);
        return new Reflections(builder);
    }
}
