//package org.example.dcheck.impl;
//
//import lombok.NonNull;
//import lombok.var;
//import org.springframework.util.ReflectionUtils;
//
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//import java.util.Objects;
//import java.util.concurrent.CompletableFuture;
//
///**
// * Date 2025/03/11
// * 可重复触发的CompletableFuture
// *
// * @author 三石而立Sunsy
// */
//public class RecompletableFuture<T> extends CompletableFuture<T> {
//    protected static final Method postCompleteMethod;
//
//    protected static final Field RESULT;
//
//    static {
//        try {
//            postCompleteMethod = CompletableFuture.class.getDeclaredMethod("postComplete");
//            ReflectionUtils.makeAccessible(postCompleteMethod);
//            RESULT = Objects.requireNonNull(ReflectionUtils.findField(CompletableFuture.class, "result"));
//            ReflectionUtils.makeAccessible(RESULT);
//        } catch (Exception x) {
//            throw new Error(x);
//        }
//    }
//
//    public static <T> RecompletableFuture<T> completedFuture(T value) {
//        var fu = new RecompletableFuture<T>();
//        ReflectionUtils.setField(RESULT, fu, value);
//        return fu;
//    }
//
//    @Override
//    public boolean complete(@NonNull T value) {
//        ReflectionUtils.setField(RESULT, this, value);
//        ReflectionUtils.invokeMethod(postCompleteMethod, this);
//        return true;
//    }
//}
