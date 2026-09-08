package org.jsoup.helper;

import org.jsoup.Connection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.http.HttpRequest;

/**
 Test access shim for the Java 11 HTTP client classes.
 <p>Maven compiles the production class as a multi-release JAR overlay, so the base test compilation cannot bind to it directly.
 Reflection lets the tests access that overlay under Maven and the same Java 11 implementation from an IDE's flat output.</p>
 */
final class HttpClientTestAccess {
    private static final String ExecutorClassName = "org.jsoup.helper.HttpClientExecutor";
    private static final String ProxyWrapClassName = ExecutorClassName + "$ProxyWrap";
    private HttpClientTestAccess() {}

    static boolean isHttpClientExecutor(RequestExecutor executor) {
        return executorClass().isInstance(executor);
    }

    static HttpRequest.BodyPublisher requestBody(HttpConnection.Request request) {
        // expose the publisher so we can verify that streamed bodies are created lazily
        try {
            return (HttpRequest.BodyPublisher) executorClass()
                    .getDeclaredMethod("requestBody", HttpConnection.Request.class)
                    .invoke(null, request);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not invoke HttpClientExecutor.requestBody", e);
        }
    }

    static Object client(Connection connection) {
        try {
            HttpConnection.Request request = (HttpConnection.Request) connection.request();
            RequestExecutor executor = RequestDispatch.get(request, null);
            Method method = executorClass().getDeclaredMethod("client");
            method.setAccessible(true);
            return method.invoke(executor);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not retrieve the configured HttpClient", e);
        }
    }

    static void resetSharedClient() {
        try {
            Field field = executorClass().getDeclaredField("sharedClientState");
            field.setAccessible(true);
            field.set(null, null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not reset the shared HttpClient", e);
        }
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
