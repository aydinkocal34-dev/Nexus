package com.nexus.app;

import android.os.Handler;
import android.os.Looper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.json.JSONObject;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public final class RelayClient {
    public interface Listener {
        void onConnected();
        void onPreKeyRequest(String id,String from,long expiresAt);
        void onPreKey(String id,String from,String bundle,long expiresAt);
        void onCiphertext(String id,String from,int fromDeviceId,byte[] ciphertext,int ciphertextType,long expiresAt);
        void onDelivery(String id,String status);
        void onError(String message);
        void onClosed();
    }

    private final OkHttpClient client=new OkHttpClient.Builder()
            .readTimeout(0,TimeUnit.MILLISECONDS)
            .pingInterval(15,TimeUnit.SECONDS)
            .build();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Listener listener;
    private WebSocket socket;
    private volatile boolean connected=false;
    private volatile boolean closing=false;
    private String relayUrl;
    private String peerId;
    private int reconnectAttempt=0;
    private final Object reconnectLock=new Object();

    public RelayClient(Listener listener){this.listener=listener;}

    public synchronized void connect(String wsUrl,String peerId){
        this.relayUrl=wsUrl;
        this.peerId=peerId;
        this.closing=false;
        reconnectAttempt=0;
        if(socket!=null) socket.close(1000,"reconnecting");
        connectInternal();
    }

    private synchronized void connectInternal(){
        final String wsUrl=relayUrl;
        final String expectedPeerId=peerId;
        if(wsUrl==null||!wsUrl.startsWith("wss://")){main.post(()->listener.onError("Relay requires WSS/TLS"));return;}
        if(expectedPeerId==null||expectedPeerId.length()<8||expectedPeerId.length()>128){main.post(()->listener.onError("Invalid peer ID"));return;}
        if(closing||connected)return;

        Request request=new Request.Builder().url(wsUrl).build();
        final WebSocket[] holder=new WebSocket[1];
        WebSocket newSocket=client.newWebSocket(request,new WebSocketListener(){
            private boolean current(){return socket==holder[0];}

            @Override public void onOpen(WebSocket webSocket,Response response){
                if(!current()){webSocket.close(1000,"stale");return;}
                try{
                    JSONObject msg=new JSONObject();
                    msg.put("type","register");
                    msg.put("peerId",expectedPeerId);
                    webSocket.send(msg.toString());
                }catch(Exception e){main.post(()->listener.onError(e.getMessage()));}
            }

            @Override public void onMessage(WebSocket webSocket,String text){
                if(!current())return;
                try{
                    JSONObject msg=new JSONObject(text);
                    String type=msg.optString("type");
                    if("registered".equals(type)){
                        connected=true;
                        reconnectAttempt=0;
                        main.post(listener::onConnected);
                    }else if("prekey-request".equals(type)){
                        String id=msg.getString("id"),from=msg.getString("from");
                        long expiresAt=msg.getLong("expiresAt");
                        main.post(()->listener.onPreKeyRequest(id,from,expiresAt));
                    }else if("ciphertext".equals(type)){
                        String id=msg.getString("id"),from=msg.getString("from");
                        byte[] ciphertext=Base64.getDecoder().decode(msg.getString("ciphertext"));
                        long expiresAt=msg.getLong("expiresAt");
                        int fromDeviceId=msg.optInt("fromDeviceId",1);
                        int ciphertextType=msg.optInt("ciphertextType",0);
                        main.post(()->listener.onCiphertext(id,from,fromDeviceId,ciphertext,ciphertextType,expiresAt));
                    }else if("prekey".equals(type)){
                        String id=msg.getString("id"),from=msg.getString("from"),bundle=msg.getString("bundle");
                        long expiresAt=msg.getLong("expiresAt");
                        main.post(()->listener.onPreKey(id,from,bundle,expiresAt));
                    }else if("delivery".equals(type)){
                        String id=msg.getString("id"),status=msg.getString("status");
                        main.post(()->listener.onDelivery(id,status));
                    }else if("error".equals(type)){
                        main.post(()->listener.onError(msg.optString("code","RELAY_ERROR")));
                    }
                }catch(Exception e){main.post(()->listener.onError("Invalid relay frame"));}
            }

            @Override public void onFailure(WebSocket webSocket,Throwable t,Response response){
                if(!current())return;
                connected=false;
                if(!closing)scheduleReconnect();
            }

            @Override public void onClosed(WebSocket webSocket,int code,String reason){
                if(!current())return;
                connected=false;
                if(!closing)scheduleReconnect();
                main.post(listener::onClosed);
            }
        });
        holder[0]=newSocket;
        socket=newSocket;
    }

    public boolean sendCiphertext(String id,String toPeerId,byte[] ciphertext,long ttlMs){
        return sendCiphertext(id,toPeerId,ciphertext,0,1,ttlMs);
    }

    public boolean sendCiphertext(String id,String toPeerId,byte[] ciphertext,int ciphertextType,int fromDeviceId,long ttlMs){
        if(socket==null||!connected)return false;
        try{
            JSONObject msg=new JSONObject();
            msg.put("type","relay");msg.put("id",id);msg.put("to",toPeerId);
            msg.put("ciphertext",Base64.getEncoder().encodeToString(ciphertext));
            msg.put("ciphertextType",ciphertextType);msg.put("fromDeviceId",fromDeviceId);
            msg.put("ttlMs",Math.max(1000L,ttlMs));
            return socket.send(msg.toString());
        }catch(Exception e){main.post(()->listener.onError("Could not encode relay frame"));return false;}
    }

    public boolean requestPreKey(String id,String toPeerId,long ttlMs){
        if(socket==null||!connected)return false;
        try{
            JSONObject msg=new JSONObject();
            msg.put("type","prekey-request");msg.put("id",id);msg.put("to",toPeerId);msg.put("ttlMs",Math.max(1000L,ttlMs));
            return socket.send(msg.toString());
        }catch(Exception e){main.post(()->listener.onError("Could not encode prekey request"));return false;}
    }

    public boolean sendPreKey(String id,String toPeerId,String bundle,long ttlMs){
        if(socket==null||!connected)return false;
        try{
            JSONObject msg=new JSONObject();
            msg.put("type","prekey");msg.put("id",id);msg.put("to",toPeerId);msg.put("bundle",bundle);msg.put("ttlMs",Math.max(1000L,ttlMs));
            return socket.send(msg.toString());
        }catch(Exception e){main.post(()->listener.onError("Could not encode prekey frame"));return false;}
    }

    public boolean sendReceivedReceipt(String id,String toPeerId){
        if(socket==null||!connected)return false;
        try{
            JSONObject msg=new JSONObject();
            msg.put("type","received");msg.put("id",id);msg.put("to",toPeerId);
            return socket.send(msg.toString());
        }catch(Exception e){main.post(()->listener.onError("Could not encode delivery receipt"));return false;}
    }

    public boolean sendReadReceipt(String id,String toPeerId){
        if(socket==null||!connected)return false;
        try{
            JSONObject msg=new JSONObject();
            msg.put("type","ack");msg.put("id",id);msg.put("to",toPeerId);
            return socket.send(msg.toString());
        }catch(Exception e){main.post(()->listener.onError("Could not encode read receipt"));return false;}
    }

    private void scheduleReconnect(){
        synchronized(reconnectLock){
            if(closing)return;
            int attempt=++reconnectAttempt;
            long delay=Math.min(15000L,500L << Math.min(attempt-1,5));
            main.postDelayed(()->{if(!closing&&!connected)connectInternal();},delay);
        }
    }

    public void close(){
        closing=true;connected=false;
        if(socket!=null)socket.close(1000,"client closing");
        client.dispatcher().executorService().shutdown();
    }
}