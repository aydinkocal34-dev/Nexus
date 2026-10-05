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

/**
 * Thin transport client. It never accepts or exposes plaintext message content.
 * The ciphertext supplied to sendCiphertext() is produced by libsignal.
 */
public final class RelayClient {
    public interface Listener {
        void onConnected();
        void onPreKeyRequest(String id, String from, long expiresAt);
        void onPreKey(String id, String from, String bundle, long expiresAt);
        void onCiphertext(String id, String from, int fromDeviceId, byte[] ciphertext, int ciphertextType, long expiresAt);
        void onDelivery(String id, String status);
        void onError(String message);
        void onClosed();
    }

    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(15, TimeUnit.SECONDS)
            .build();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private WebSocket socket;
    private volatile boolean connected=false;

    public RelayClient(Listener listener) {
        this.listener = listener;
    }

    public void connect(String wsUrl, String peerId) {
        if (wsUrl == null || !wsUrl.startsWith("wss://")) {
            main.post(() -> listener.onError("Relay requires WSS/TLS"));
            return;
        }
        if (peerId == null || peerId.length() < 8 || peerId.length() > 128) {
            main.post(() -> listener.onError("Invalid peer ID"));
            return;
        }
        Request request = new Request.Builder().url(wsUrl).build();
        socket = client.newWebSocket(request, new WebSocketListener() {
            @Override public void onOpen(WebSocket webSocket, Response response) {
                JSONObject msg = new JSONObject();
                try {
                    msg.put("type", "register");
                    msg.put("peerId", peerId);
                    webSocket.send(msg.toString());
                } catch (Exception e) {
                    main.post(() -> listener.onError(e.getMessage()));
                }
            }

            @Override public void onMessage(WebSocket webSocket, String text) {
                try {
                    JSONObject msg = new JSONObject(text);
                    String type = msg.optString("type");
                    if ("registered".equals(type)) {
                        connected=true; main.post(listener::onConnected);
                    } else if ("prekey-request".equals(type)) {
                        String id = msg.getString("id");
                        String from = msg.getString("from");
                        long expiresAt = msg.getLong("expiresAt");
                        main.post(() -> listener.onPreKeyRequest(id, from, expiresAt));
                    } else if ("ciphertext".equals(type)) {
                        String id = msg.getString("id");
                        String from = msg.getString("from");
                        byte[] ciphertext = Base64.getDecoder().decode(msg.getString("ciphertext"));
                        long expiresAt = msg.getLong("expiresAt");
                        int fromDeviceId = msg.optInt("fromDeviceId", 1);
                        main.post(() -> listener.onCiphertext(id, from, fromDeviceId, ciphertext, msg.optInt("ciphertextType", 0), expiresAt));
                    } else if ("prekey".equals(type)) {
                        String id = msg.getString("id");
                        String from = msg.getString("from");
                        String bundle = msg.getString("bundle");
                        long expiresAt = msg.getLong("expiresAt");
                        main.post(() -> listener.onPreKey(id, from, bundle, expiresAt));
                    } else if ("delivery".equals(type)) {
                        String id = msg.getString("id");
                        String status = msg.getString("status");
                        main.post(() -> listener.onDelivery(id, status));
                    } else if ("error".equals(type)) {
                        main.post(() -> listener.onError(msg.optString("code", "RELAY_ERROR")));
                    }
                } catch (Exception e) {
                    main.post(() -> listener.onError("Invalid relay frame"));
                }
            }

            @Override public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                connected=false; main.post(() -> listener.onError(t.getMessage() == null ? "Relay connection failed" : t.getMessage()));
            }

            @Override public void onClosed(WebSocket webSocket, int code, String reason) {
                connected=false; main.post(listener::onClosed);
            }
        });
    }

    public boolean sendCiphertext(String id, String toPeerId, byte[] ciphertext, long ttlMs) {
        return sendCiphertext(id, toPeerId, ciphertext, 0, 1, ttlMs);
    }

    public boolean sendCiphertext(String id, String toPeerId, byte[] ciphertext, int ciphertextType, int fromDeviceId, long ttlMs) {
        if (socket == null || !connected) return false;
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "relay");
            msg.put("id", id);
            msg.put("to", toPeerId);
            msg.put("ciphertext", Base64.getEncoder().encodeToString(ciphertext));
            msg.put("ciphertextType", ciphertextType);
            msg.put("fromDeviceId", fromDeviceId);
            msg.put("ttlMs", Math.max(1000L, ttlMs));
            return socket.send(msg.toString());
        } catch (Exception e) {
            main.post(() -> listener.onError("Could not encode relay frame"));
            return false;
        }
    }

    public boolean requestPreKey(String id, String toPeerId, long ttlMs) {
        if (socket == null || !connected) return false;
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "prekey-request");
            msg.put("id", id);
            msg.put("to", toPeerId);
            msg.put("ttlMs", Math.max(1000L, ttlMs));
            return socket.send(msg.toString());
        } catch (Exception e) {
            main.post(() -> listener.onError("Could not encode prekey request"));
            return false;
        }
    }

    public boolean sendPreKey(String id, String toPeerId, String bundle, long ttlMs) {
        if (socket == null || !connected) return false;
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "prekey");
            msg.put("id", id);
            msg.put("to", toPeerId);
            msg.put("bundle", bundle);
            msg.put("ttlMs", Math.max(1000L, ttlMs));
            return socket.send(msg.toString());
        } catch (Exception e) {
            main.post(() -> listener.onError("Could not encode prekey frame"));
            return false;
        }
    }

    public boolean sendReadReceipt(String id, String toPeerId) {
        if (socket == null || !connected) return false;
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "ack");
            msg.put("id", id);
            msg.put("to", toPeerId);
            return socket.send(msg.toString());
        } catch (Exception e) {
            main.post(() -> listener.onError("Could not encode read receipt"));
            return false;
        }
    }

    public void close() {
        if (socket != null) socket.close(1000, "client closing");
        client.dispatcher().executorService().shutdown();
    }
}