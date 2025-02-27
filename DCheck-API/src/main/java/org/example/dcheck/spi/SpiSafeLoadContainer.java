package org.example.dcheck.spi;

import lombok.extern.slf4j.Slf4j;

/**
 * Date: 2025/2/28
 *
 * @author 三石而立Sunsy
 */
@Slf4j
@SuppressWarnings("unused")
public class SpiSafeLoadContainer {

//    @Nullable
//    @SuppressWarnings("unchecked")
//    public static <Service> Service create(Class<Service> serviceClass, String implClassname) {
//        Class<?> candidate;
//        try {
//            candidate = Class.forName(implClassname);
//        } catch (Throwable e) {
//            log.warn("[Spi Safe Load]: fail to load class '{}'", implClassname, e);
//            return null;
//        }
//        try {
//            return (Service) candidate.getConstructor().newInstance();
//        } catch (IllegalAccessException |
//                 NoSuchMethodException e) {
//            throw new IllegalStateException("[Spi Safe Load]: fail to new service instant: " + e.getMessage(), e);
//        } catch (InvocationTargetException e) {
//            throw new RuntimeException(e);
//        } catch (InstantiationException e) {
//            //TODO 请帮我找到最根源的异常但是不要因为根源异常是自己导致死循环，写代码吧：
//
//
//
//            e.getCause();
//
//
//            e.getCause();
//        }
//    }
}
