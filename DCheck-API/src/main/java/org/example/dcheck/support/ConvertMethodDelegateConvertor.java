package org.example.dcheck.support;

import lombok.Getter;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Date: 2025/3/18
 *
 * @author 三石而立Sunsy
 */
@Getter
public class ConvertMethodDelegateConvertor implements GenericConverter {
    private final Set<ConvertiblePair> convertibleTypes = Collections.singleton(new ConvertiblePair(Object.class, Object.class));

    private final ConcurrentReferenceHashMap<CacheKey, Method> parseMethods = new ConcurrentReferenceHashMap<>(32);

    @Getter
    private final List<String> delegateMethodNames = new ArrayList<>(Arrays.asList("parse", "of", "from", "valueOf", "create"));

    protected Method getParseMethod(Class<?> sourceClass, Class<?> targetClass) {
        Method method = parseMethods.get(new CacheKey(sourceClass, targetClass));
        if (method != null) return method;
        Method parseMethod = getDelegate(sourceClass, targetClass);
        if (parseMethod != null) {
            parseMethods.put(new CacheKey(sourceClass, targetClass), parseMethod);
        }
        return parseMethod;
    }

    @Nullable
    protected Method getDelegate(Class<?> sourceClass, Class<?> targetClass) {
        return Arrays.stream(ReflectionUtils.getAllDeclaredMethods(targetClass))
                .filter(m -> delegateMethodNames.contains(m.getName())
                        && Modifier.isStatic(m.getModifiers())
                        && m.getParameterCount() == 1 && targetClass.isAssignableFrom(m.getReturnType())
                        && m.getParameterTypes()[0].isAssignableFrom(sourceClass)
                )
                .findFirst()
                .orElse(null);
    }

    @Override
    public Object convert(Object source, @NotNull TypeDescriptor sourceType, @NotNull TypeDescriptor targetType) {
        if (source == null) return null;
        Class<?> sourceClass = sourceType.getType();
        Class<?> targetClass = targetType.getType();
        Method parseMethod = getParseMethod(sourceClass, targetClass);
        if (parseMethod == null) {
            throw new ConversionFailedException(
                    sourceType,
                    targetType,
                    source,
                    new NoSuchMethodException("no static delegate method in target class '" + targetClass + "': " + delegateMethodNames));
        }
        return ReflectionUtils.invokeMethod(parseMethod, null, source);
    }

    @Value
    protected static class CacheKey {
        Class<?> sourceType;
        Class<?> targetType;
    }
}
