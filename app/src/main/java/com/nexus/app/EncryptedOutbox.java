package com.nexus.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Small crash-safe encrypted outbox for messages waiting on Signal session/relay.
 * Plaintext is never persisted; the queue is AES-GCM encrypted with an Android Keystore key.
 */
final class EncryptedOutbox {
    static final class Item {
        final String id, peerId, text;
        final int deviceId;
        Item(String id,String peerId,int deviceId,String text){
            this.id=id;this.peerId=peerId;this.deviceId=deviceId;this.text=text;
        }
    }

    private static final String PREFS="nexus_secure_outbox";
    private static final String KEY_ALIAS="nexus_secure_outbox_key";
    private static final String FIELD="payload";
    private final SharedPreferences prefs;

    EncryptedOutbox(Context context){
        prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        ensureKey();
    }

    synchronized List<Item> load(){
        ArrayList<Item> out=new ArrayList<>();
        String packed=prefs.getString(FIELD,null);
        if(packed==null||packed.isEmpty())return out;
        try{
            byte[] all=Base64.decode(packed,Base64.NO_WRAP);
            if(all.length<13)return out;
            byte[] iv=new byte[12];
            byte[] cipher=new byte[all.length-12];
            System.arraycopy(all,0,iv,0,12);
            System.arraycopy(all,12,cipher,0,cipher.length);
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE,getKey(),new GCMParameterSpec(128,iv));
            JSONArray a=new JSONArray(new String(c.doFinal(cipher),StandardCharsets.UTF_8));
            for(int i=0;i<a.length();i++){
                JSONObject o=a.getJSONObject(i);
                out.add(new Item(o.getString("id"),o.getString("peerId"),o.optInt("deviceId",1),o.getString("text")));
            }
        }catch(Exception ignored){
            prefs.edit().remove(FIELD).apply();
        }
        return out;
    }

    synchronized void save(List<Item> items){
        try{
            JSONArray a=new JSONArray();
            for(Item p:items){
                JSONObject o=new JSONObject();
                o.put("id",p.id);o.put("peerId",p.peerId);o.put("deviceId",p.deviceId);o.put("text",p.text);
                a.put(o);
            }
            byte[] iv=new byte[12];
            new java.security.SecureRandom().nextBytes(iv);
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE,getKey(),new GCMParameterSpec(128,iv));
            byte[] cipher=c.doFinal(a.toString().getBytes(StandardCharsets.UTF_8));
            byte[] all=new byte[iv.length+cipher.length];
            System.arraycopy(iv,0,all,0,iv.length);
            System.arraycopy(cipher,0,all,iv.length,cipher.length);
            prefs.edit().putString(FIELD,Base64.encodeToString(all,Base64.NO_WRAP)).apply();
        }catch(Exception ignored){}
    }

    private SecretKey getKey() throws Exception{
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        return ((KeyStore.SecretKeyEntry)ks.getEntry(KEY_ALIAS,null)).getSecretKey();
    }

    private void ensureKey(){
        try{
            KeyStore ks=KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            if(ks.containsAlias(KEY_ALIAS))return;
            KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build());
            kg.generateKey();
        }catch(Exception ignored){}
    }
}
