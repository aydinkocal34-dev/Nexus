package com.nexus.app;

import android.content.Context;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

final class E2eeRuntime {
    interface Listener {
        void onReady();
        void onMessage(String from,String message,String id);
        void onDelivery(String id,String status);
        void onClosed();
        void onError(String message);
    }

    private final Object transport;
    private final Method connectMethod;
    private final Method sendTextMethod;

    E2eeRuntime(Context context,String localPeerId,Listener listener) throws Exception {
        Class<?> transportClass=Class.forName("com.nexus.app.RealE2eeTransport");
        Class<?> listenerClass=Class.forName("com.nexus.app.RealE2eeTransport$Listener");
        Object proxy=Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class<?>[]{listenerClass},
                (obj,method,args)->{
                    String n=method.getName();
                    if("onReady".equals(n)) listener.onReady();
                    else if("onMessage".equals(n) && args!=null && args.length>=3)
                        listener.onMessage(String.valueOf(args[0]),String.valueOf(args[1]),String.valueOf(args[2]));
                    else if("onDelivery".equals(n) && args!=null && args.length>=2)
                        listener.onDelivery(String.valueOf(args[0]),String.valueOf(args[1]));
                    else if("onClosed".equals(n)) listener.onClosed();
                    else if("onError".equals(n) && args!=null && args.length>=1)
                        listener.onError(String.valueOf(args[0]));
                    return null;
                });
        Constructor<?> ctor=transportClass.getConstructor(Context.class,String.class,int.class,listenerClass);
        transport=ctor.newInstance(context,localPeerId,1,proxy);
        connectMethod=transportClass.getMethod("connect",String.class);
        sendTextMethod=transportClass.getMethod("sendText",String.class,int.class,String.class);
    }

    void connect(String url){
        try { connectMethod.invoke(transport,url); }
        catch(Exception e){ throw new RuntimeException(e.getCause()==null?e:e.getCause()); }
    }

    String sendText(String peer,int deviceId,String text){
        try { return String.valueOf(sendTextMethod.invoke(transport,peer,deviceId,text)); }
        catch(Exception e){ throw new RuntimeException(e.getCause()==null?e:e.getCause()); }
    }
}
