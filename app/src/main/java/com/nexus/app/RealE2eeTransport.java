package com.nexus.app;

import android.content.Context;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RealE2eeTransport implements RelayClient.Listener {
    public interface Listener {
        void onReady();
        void onMessage(String fromPeerId, String message, String messageId);
        void onDelivery(String id, String status);
        void onError(String message);
    }
    private static final class Pending {
        final String id, peerId, text;
        final int deviceId;
        Pending(String id,String peerId,int deviceId,String text){this.id=id;this.peerId=peerId;this.deviceId=deviceId;this.text=text;}
    }
    private final DeviceE2eeController e2ee;
    private final RelayClient relay;
    private final String localPeerId;
    private final int localDeviceId;
    private final Listener listener;
    private final List<Pending> pending=new ArrayList<>();
    private final List<PendingPreKey> pendingPreKeys=new ArrayList<>();
    private volatile boolean relayReady=false;
    private static final class PendingPreKey { final String id,toPeer,bundle; PendingPreKey(String id,String toPeer,String bundle){this.id=id;this.toPeer=toPeer;this.bundle=bundle;} }

    public RealE2eeTransport(Context context,String localPeerId,int localDeviceId,Listener listener)throws Exception{
        if(localPeerId==null||localPeerId.length()<8)throw new IllegalArgumentException("Invalid local peer ID");
        this.localPeerId=localPeerId; this.localDeviceId=localDeviceId; this.listener=listener;
        this.e2ee=new DeviceE2eeController(context,localPeerId,localDeviceId);
        this.relay=new RelayClient(this);
    }
    public void connect(String wssUrl){relay.connect(wssUrl,localPeerId);}
    public void requestSession(String remotePeerId){
        if(!relayReady){return;}
        if(!relay.requestPreKey(UUID.randomUUID().toString(),remotePeerId,60000L)) listener.onError("Pre-key request could not be sent");
    }
    public synchronized String sendText(String remotePeerId,int remoteDeviceId,String text){
        String id=UUID.randomUUID().toString();
        if(!relayReady){pending.add(new Pending(id,remotePeerId,remoteDeviceId,text));return id;}
        try{
            DeviceE2eeController.CipherPacket p=e2ee.encrypt(remotePeerId,remoteDeviceId,text.getBytes(StandardCharsets.UTF_8));
            if(!relay.sendCiphertext(id,remotePeerId,p.bytes,p.type,localDeviceId,60000L)) throw new IllegalStateException("Relay send rejected");
        }catch(Exception ex){pending.add(new Pending(id,remotePeerId,remoteDeviceId,text));requestSession(remotePeerId);}
        return id;
    }
    private synchronized void flushPending(String peerId,int deviceId){
        for(int i=pending.size()-1;i>=0;i--){
            Pending p=pending.get(i); if(!p.peerId.equals(peerId)||p.deviceId!=deviceId)continue;
            try{
                DeviceE2eeController.CipherPacket cp=e2ee.encrypt(p.peerId,p.deviceId,p.text.getBytes(StandardCharsets.UTF_8));
                if(relay.sendCiphertext(p.id,p.peerId,cp.bytes,cp.type,localDeviceId,60000L)) pending.remove(i);
            }catch(Exception ex){listener.onError("E2EE pending send failed: "+ex.getMessage());}
        }
    }
    public void close(){relay.close();}
    @Override public void onConnected(){
        relayReady=true;
        listener.onReady();
        java.util.HashSet<String> peers=new java.util.HashSet<>();
        synchronized(this){for(Pending p:pending) peers.add(p.peerId);}
        for(String peer:peers) requestSession(peer);
        flushPendingPreKeys();
    }
    @Override public synchronized void onPreKeyRequest(String id,String from,long expiresAt){
        try{SignalPreKeyEnvelope b=e2ee.createLocalPreKeyBundle(); String encoded=b.encode(); if(!relay.sendPreKey(id,from,encoded,60000L)) pendingPreKeys.add(new PendingPreKey(id,from,encoded));}
        catch(Exception e){listener.onError("Pre-key generation failed: "+e.getMessage());}
    }
    private synchronized void flushPendingPreKeys(){
        for(int i=pendingPreKeys.size()-1;i>=0;i--){PendingPreKey p=pendingPreKeys.get(i); if(relay.sendPreKey(p.id,p.toPeer,p.bundle,60000L)) pendingPreKeys.remove(i);}
    }
    @Override public void onPreKey(String id,String from,String bundle,long expiresAt){
        try{SignalPreKeyEnvelope e=SignalPreKeyEnvelope.decode(bundle);e2ee.establishSession(from,e.deviceId,e);flushPending(from,e.deviceId);listener.onReady();}
        catch(Exception e){listener.onError("E2EE session setup failed: "+e.getMessage());}
    }
    @Override public void onCiphertext(String id,String from,int fromDeviceId,byte[] ciphertext,int type,long expiresAt){
        try{byte[] clear=e2ee.decrypt(from,fromDeviceId,type,ciphertext);listener.onMessage(from,new String(clear,StandardCharsets.UTF_8),id);relay.sendReadReceipt(id,from);}
        catch(Exception e){listener.onError("E2EE decrypt failed: "+e.getMessage());}
    }
    @Override public void onDelivery(String id,String status){listener.onDelivery(id,status);}
    @Override public void onError(String message){listener.onError(message);}
    @Override public void onClosed(){relayReady=false;}
}