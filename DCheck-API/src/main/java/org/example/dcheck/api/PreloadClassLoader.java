package org.example.dcheck.api;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Date 2025/03/12
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
        var reflections = buildReflections();
        try {
            var preloaded = reflections.getSubTypesOf(PreloadClass.class);
            for (Class<? extends PreloadClass> clazz : preloaded) {
                for (Method method : ReflectionUtils.getDeclaredMethods(clazz)) {
                    if (AnnotationUtils.findAnnotation(method, PreloadClass.PreloadMethod.class) != null) {
                        if (method.getParameterCount() != 0 || !Modifier.isStatic(method.getModifiers())) {
                            throw new IllegalArgumentException("@PreloadClass.PreloadMethod method must be static and no parameters: " + method);
                        }
                        ReflectionUtils.makeAccessible(method);
                        ReflectionUtils.invokeMethod(method, null);
                    }
                }
            }
            log.info("preload PreloadClass success: {}", preloaded);
        } catch (Throwable e) {
            throw new IllegalStateException("preload fail: " + e.getMessage(), e);
        }
    }

    @NotNull
    protected Reflections buildReflections() {
        var classLoaders = this.classLoaders.toArray(new ClassLoader[0]);
        var builder = new ConfigurationBuilder();
        for (String pkg : packages) {
            builder.addUrls(ClasspathHelper.forPackage(pkg, classLoaders));
        }
        builder.addScanners(Scanners.SubTypes);
        builder.addClassLoaders(classLoaders);
        return new Reflections(builder);
    }
}
