package com.nexus.app;

import org.signal.libsignal.protocol.IdentityKey;
import org.signal.libsignal.protocol.IdentityKeyPair;
import org.signal.libsignal.protocol.NoSessionException;
import org.signal.libsignal.protocol.ReusedBaseKeyException;
import org.signal.libsignal.protocol.SignalProtocolAddress;
import org.signal.libsignal.protocol.InvalidKeyIdException;
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord;
import org.signal.libsignal.protocol.state.KyberPreKeyRecord;
import org.signal.libsignal.protocol.state.PreKeyRecord;
import org.signal.libsignal.protocol.state.SessionRecord;
import org.signal.libsignal.protocol.state.SignedPreKeyRecord;
import org.signal.libsignal.protocol.state.impl.InMemorySignalProtocolStore;
import org.signal.libsignal.protocol.ecc.ECPublicKey;

import android.content.Context;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.UUID;
import java.util.UUID;

/**
 * Persistent Signal store backed by Android Keystore-encrypted state.
 * Libsignal record serialization remains owned by libsignal; this class only persists
 * those opaque records and never implements cryptography itself.
 */
public final class PersistentSignalProtocolStore extends InMemorySignalProtocolStore {
    private final EncryptedStateStore storage;
    private final String namespace;

    public PersistentSignalProtocolStore(Context context, String namespace) throws Exception {
        super(loadIdentity(context, namespace), loadRegistrationId(context, namespace));
        this.storage = new EncryptedStateStore(context);
        this.namespace = namespace;
    }

    private static IdentityKeyPair loadIdentity(Context context, String ns) throws Exception {
        EncryptedStateStore s = new EncryptedStateStore(context);
        byte[] bytes = s.get(key(ns, "identity"));
        if (bytes != null) return new IdentityKeyPair(bytes);
        IdentityKeyPair pair = IdentityKeyPair.generate();
        s.put(key(ns, "identity"), pair.serialize());
        return pair;
    }

    private static int loadRegistrationId(Context context, String ns) throws Exception {
        EncryptedStateStore s = new EncryptedStateStore(context);
        byte[] bytes = s.get(key(ns, "registration"));
        if (bytes != null) return Integer.parseInt(new String(bytes, StandardCharsets.UTF_8));
        int id = 1 + new SecureRandom().nextInt(16379);
        s.put(key(ns, "registration"), Integer.toString(id).getBytes(StandardCharsets.UTF_8));
        return id;
    }

    private static String key(String ns, String suffix) {
        return "signal/" + ns + "/" + suffix;
    }

    private String k(String type, String id) {
        return key(namespace, type + "/" + id);
    }

    private static String addressId(SignalProtocolAddress a) {
        return Base64.encodeToString(a.getName().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP)
                + "_" + a.getDeviceId();
    }

    private byte[] get(String k) throws Exception { return storage.get(k); }
    private void put(String k, byte[] v) throws Exception { storage.put(k, v); }
    private void remove(String k) { storage.remove(k); }

    private List<String> index(String type) throws Exception {
        byte[] b = get(k("index", type));
        List<String> out = new ArrayList<>();
        if (b == null || b.length == 0) return out;
        for (String x : new String(b, StandardCharsets.UTF_8).split("\n")) {
            if (!x.isEmpty()) out.add(x);
        }
        return out;
    }

    private void addIndex(String type, String id) throws Exception {
        List<String> xs = index(type);
        if (!xs.contains(id)) xs.add(id);
        put(k("index", type), String.join("\n", xs).getBytes(StandardCharsets.UTF_8));
    }

    private void removeIndex(String type, String id) throws Exception {
        List<String> xs = index(type);
        xs.remove(id);
        put(k("index", type), String.join("\n", xs).getBytes(StandardCharsets.UTF_8));
    }

    @Override public void storeSession(SignalProtocolAddress a, SessionRecord r) {
        super.storeSession(a, r);
        try { put(k("session", addressId(a)), r.serialize()); addIndex("session", addressId(a)); }
        catch (Exception e) { throw new IllegalStateException("Persistent session write failed", e); }
    }

    @Override public SessionRecord loadSession(SignalProtocolAddress a) {
        try {
            byte[] b = get(k("session", addressId(a)));
            if (b != null) {
                SessionRecord r = new SessionRecord(b);
                super.storeSession(a, r);
                return r;
            }
        } catch (Exception e) { throw new IllegalStateException("Persistent session read failed", e); }
        return super.loadSession(a);
    }

    @Override public boolean containsSession(SignalProtocolAddress a) {
        try { if (get(k("session", addressId(a))) != null) return true; }
        catch (Exception e) { throw new IllegalStateException(e); }
        return super.containsSession(a);
    }

    @Override public void deleteSession(SignalProtocolAddress a) {
        super.deleteSession(a);
        try { remove(k("session", addressId(a))); removeIndex("session", addressId(a)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    @Override public void deleteAllSessions(String name) {
        try {
            for (String id : index("session")) {
                byte[] b = get(k("session", id));
                if (b != null) {
                    // index entries encode the name, so only remove matching decoded prefix.
                    String encodedName = id.substring(0, id.lastIndexOf('_'));
                    String decoded = new String(Base64.decode(encodedName, Base64.NO_WRAP), StandardCharsets.UTF_8);
                    if (decoded.equals(name)) { remove(k("session", id)); removeIndex("session", id); }
                }
            }
        } catch (Exception e) { throw new IllegalStateException(e); }
        super.deleteAllSessions(name);
    }

    @Override public void storePreKey(int id, PreKeyRecord r) {
        super.storePreKey(id, r);
        try { put(k("prekey", Integer.toString(id)), r.serialize()); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    @Override public PreKeyRecord loadPreKey(int id) throws InvalidKeyIdException {
        try {
            byte[] b = get(k("prekey", Integer.toString(id)));
            if (b != null) return new PreKeyRecord(b);
        } catch (Exception e) { throw new IllegalStateException(e); }
        return super.loadPreKey(id);
    }

    @Override public boolean containsPreKey(int id) {
        try { if (get(k("prekey", Integer.toString(id))) != null) return true; }
        catch (Exception e) { throw new IllegalStateException(e); }
        return super.containsPreKey(id);
    }

    @Override public void removePreKey(int id) {
        super.removePreKey(id); remove(k("prekey", Integer.toString(id)));
    }

    @Override public void storeSignedPreKey(int id, SignedPreKeyRecord r) {
        super.storeSignedPreKey(id, r);
        try { put(k("signed", Integer.toString(id)), r.serialize()); addIndex("signed", Integer.toString(id)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    @Override public SignedPreKeyRecord loadSignedPreKey(int id) throws InvalidKeyIdException {
        try {
            byte[] b = get(k("signed", Integer.toString(id)));
            if (b != null) return new SignedPreKeyRecord(b);
        } catch (Exception e) { throw new IllegalStateException(e); }
        return super.loadSignedPreKey(id);
    }

    @Override public List<SignedPreKeyRecord> loadSignedPreKeys() {
        try {
            List<SignedPreKeyRecord> out = new ArrayList<>();
            for (String id : index("signed")) {
                byte[] b = get(k("signed", id));
                if (b != null) out.add(new SignedPreKeyRecord(b));
            }
            return out;
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    @Override public boolean containsSignedPreKey(int id) {
        try { return get(k("signed", Integer.toString(id))) != null; }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    @Override public void removeSignedPreKey(int id) {
        super.removeSignedPreKey(id);
        try { remove(k("signed", Integer.toString(id))); removeIndex("signed", Integer.toString(id)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    @Override public void storeKyberPreKey(int id, KyberPreKeyRecord r) {
        super.storeKyberPreKey(id, r);
        try { put(k("kyber", Integer.toString(id)), r.serialize()); addIndex("kyber", Integer.toString(id)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    @Override public KyberPreKeyRecord loadKyberPreKey(int id) throws InvalidKeyIdException {
        try {
            byte[] b = get(k("kyber", Integer.toString(id)));
            if (b != null) return new KyberPreKeyRecord(b);
        } catch (Exception e) { throw new IllegalStateException(e); }
        return super.loadKyberPreKey(id);
    }

    @Override public List<KyberPreKeyRecord> loadKyberPreKeys() {
        try {
            List<KyberPreKeyRecord> out = new ArrayList<>();
            for (String id : index("kyber")) {
                byte[] b = get(k("kyber", id));
                if (b != null) out.add(new KyberPreKeyRecord(b));
            }
            return out;
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    @Override public boolean containsKyberPreKey(int id) {
        try { return get(k("kyber", Integer.toString(id))) != null; }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    public void removeKyberPreKey(int id) {
        try { remove(k("kyber", Integer.toString(id))); removeIndex("kyber", Integer.toString(id)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    @Override public void storeSenderKey(SignalProtocolAddress a, UUID distributionId, SenderKeyRecord r) {
        super.storeSenderKey(a, distributionId, r);
        try { put(k("sender", addressId(a) + "_" + distributionId), r.serialize()); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    @Override public SenderKeyRecord loadSenderKey(SignalProtocolAddress a, UUID distributionId) {
        try {
            byte[] b = get(k("sender", addressId(a) + "_" + distributionId));
            if (b != null) return new SenderKeyRecord(b);
        } catch (Exception e) { throw new IllegalStateException(e); }
        return super.loadSenderKey(a, distributionId);
    }

    @Override public void markKyberPreKeyUsed(int id, int signedId, ECPublicKey baseKey)
            throws ReusedBaseKeyException {
        String marker = k("kyber-used", id + "/" + signedId + "/" +
                Base64.encodeToString(baseKey.serialize(), Base64.NO_WRAP));
        try {
            if (get(marker) != null) throw new ReusedBaseKeyException("Kyber pre-key reuse detected");
            super.markKyberPreKeyUsed(id, signedId, baseKey);
            put(marker, new byte[]{1});
        } catch (ReusedBaseKeyException e) { throw e; }
          catch (Exception e) { throw new IllegalStateException("Kyber replay-state write failed", e); }
    }

    public void clearNamespace() {
        try {
            for (String type : new String[]{"session","signed","kyber"}) {
                for (String id : index(type)) remove(k(type,id));
                remove(k("index",type));
            }
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}