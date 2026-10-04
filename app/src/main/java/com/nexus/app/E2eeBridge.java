package com.nexus.app;

import android.content.Context;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

final class E2eeBridge {
    interface Listener {
        void onReady();
        void onMessage(String from, String message, String id);
        void onError(String message);
    }

    private final Object runtime;
    private final Method connect;
    private final Method sendText;

    E2eeBridge(Context context, String localPeerId, Listener listener) throws Exception {
        Class<?> runtimeClass = Class.forName("com.nexus.app.E2eeRuntime", true, context.getClassLoader());
        Class<?> listenerClass = Class.forName("com.nexus.app.E2eeRuntime$Listener", true, context.getClassLoader());

        Object proxy = Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class<?>[]{listenerClass},
                (obj, method, args) -> {
                    String name = method.getName();
                    if ("onReady".equals(name)) listener.onReady();
                    else if ("onMessage".equals(name) && args != null && args.length >= 3)
                        listener.onMessage(String.valueOf(args[0]), String.valueOf(args[1]), String.valueOf(args[2]));
                    else if ("onError".equals(name) && args != null && args.length >= 1)
                        listener.onError(String.valueOf(args[0]));
                    return null;
                });

        Constructor<?> ctor = runtimeClass.getDeclaredConstructor(Context.class, String.class, E2eeRuntime.Listener.class);
        runtime = ctor.newInstance(context, localPeerId, proxy);
        connect = runtimeClass.getDeclaredMethod("connect", String.class);
        sendText = runtimeClass.getDeclaredMethod("sendText", String.class, int.class, String.class);
        connect.setAccessible(true);
        sendText.setAccessible(true);
    }

    void connect(String url) {
        try {
            connect.invoke(runtime, url);
        } catch (Exception e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new RuntimeException(cause);
        }
    }

    void sendText(String peer, int deviceId, String text) {
        try {
            sendText.invoke(runtime, peer, deviceId, text);
        } catch (Exception e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new RuntimeException(cause);
        }
    }
}
