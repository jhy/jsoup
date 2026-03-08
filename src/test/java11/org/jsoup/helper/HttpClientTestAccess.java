package org.jsoup.helper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URL;

/**
 Test access shim for the Java 11 multi-release classes.
 <p>These tests need to exercise the Java 11 implementation as loaded from
 {@code META-INF/versions/11}. Using reflection here keeps them bound to that
 packaged implementation, without adding the Java 11 sources to the test
 compile path.</p>
 */
final class HttpClientTestAccess {
    private static final String ExecutorClassName = "org.jsoup.helper.HttpClientExecutor";
    private static final String ProxyWrapClassName = ExecutorClassName + "$ProxyWrap";
    private static final String ExecutorClassResource = "org/jsoup/helper/HttpClientExecutor.class";

    private HttpClientTestAccess() {}

    static boolean isHttpClientExecutor(RequestExecutor executor) {
        return executorClass().isInstance(executor);
    }

    static URL executorClassResource() {
        URL resource = HttpClientTestAccess.class.getClassLoader().getResource(ExecutorClassResource);
        if (resource == null)
            throw new IllegalStateException("Could not load " + ExecutorClassResource);
        return resource;
    }

    static ProxySelector newProxyWrap() {
        try {
            Constructor<?> constructor = loadClass(ProxyWrapClassName).getDeclaredConstructor();
            constructor.setAccessible(true);
            return (ProxySelector) constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not construct HttpClientExecutor.ProxyWrap", e);
        }
    }

    static void setPerRequestProxy(Proxy proxy) {
        perRequestProxy().set(proxy);
    }

    static void clearPerRequestProxy() {
        perRequestProxy().remove();
    }

    @SuppressWarnings("unchecked")
    private static ThreadLocal<Proxy> perRequestProxy() {
        try {
            Field field = executorClass().getDeclaredField("perRequestProxy");
            field.setAccessible(true);
            return (ThreadLocal<Proxy>) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not access HttpClientExecutor.perRequestProxy", e);
        }
    }

    private static Class<?> executorClass() {
        return loadClass(ExecutorClassName);
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not load " + className, e);
        }
    }
}
