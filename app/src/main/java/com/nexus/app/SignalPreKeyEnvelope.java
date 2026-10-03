package com.nexus.app;

import android.util.Base64;
import org.json.JSONObject;
import org.signal.libsignal.protocol.IdentityKey;
import org.signal.libsignal.protocol.state.PreKeyBundle;

/**
 * Wire representation of Signal public pre-key material.
 * It contains public/session-bootstrap material only; no private keys.
 */
public final class SignalPreKeyEnvelope {
    public final int registrationId;
    public final int deviceId;
    public final int preKeyId;
    public final byte[] preKeyPublic;
    public final int signedPreKeyId;
    public final byte[] signedPreKeyPublic;
    public final byte[] signedPreKeySignature;
    public final byte[] identityKey;
    public final int kyberPreKeyId;
    public final byte[] kyberPreKeyPublic;
    public final byte[] kyberPreKeySignature;

    public SignalPreKeyEnvelope(
            int registrationId, int deviceId, int preKeyId, byte[] preKeyPublic,
            int signedPreKeyId, byte[] signedPreKeyPublic, byte[] signedPreKeySignature,
            byte[] identityKey, int kyberPreKeyId, byte[] kyberPreKeyPublic,
            byte[] kyberPreKeySignature) {
        this.registrationId = registrationId;
        this.deviceId = deviceId;
        this.preKeyId = preKeyId;
        this.preKeyPublic = preKeyPublic;
        this.signedPreKeyId = signedPreKeyId;
        this.signedPreKeyPublic = signedPreKeyPublic;
        this.signedPreKeySignature = signedPreKeySignature;
        this.identityKey = identityKey;
        this.kyberPreKeyId = kyberPreKeyId;
        this.kyberPreKeyPublic = kyberPreKeyPublic;
        this.kyberPreKeySignature = kyberPreKeySignature;
    }

    private static String b64(byte[] value) {
        return Base64.encodeToString(value, Base64.NO_WRAP);
    }

    private static byte[] unb64(String value) {
        return Base64.decode(value, Base64.NO_WRAP);
    }

    public String encode() throws Exception {
        JSONObject o = new JSONObject();
        o.put("v", 1);
        o.put("registrationId", registrationId);
        o.put("deviceId", deviceId);
        o.put("preKeyId", preKeyId);
        o.put("preKeyPublic", b64(preKeyPublic));
        o.put("signedPreKeyId", signedPreKeyId);
        o.put("signedPreKeyPublic", b64(signedPreKeyPublic));
        o.put("signedPreKeySignature", b64(signedPreKeySignature));
        o.put("identityKey", b64(identityKey));
        o.put("kyberPreKeyId", kyberPreKeyId);
        o.put("kyberPreKeyPublic", b64(kyberPreKeyPublic));
        o.put("kyberPreKeySignature", b64(kyberPreKeySignature));
        return Base64.encodeToString(o.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    public static SignalPreKeyEnvelope decode(String encoded) throws Exception {
        String json = new String(unb64(encoded), java.nio.charset.StandardCharsets.UTF_8);
        JSONObject o = new JSONObject(json);
        if (o.optInt("v", 0) != 1) throw new IllegalArgumentException("Unsupported prekey version");
        return new SignalPreKeyEnvelope(
                o.getInt("registrationId"), o.getInt("deviceId"), o.getInt("preKeyId"),
                unb64(o.getString("preKeyPublic")), o.getInt("signedPreKeyId"),
                unb64(o.getString("signedPreKeyPublic")), unb64(o.getString("signedPreKeySignature")),
                unb64(o.getString("identityKey")), o.getInt("kyberPreKeyId"),
                unb64(o.getString("kyberPreKeyPublic")), unb64(o.getString("kyberPreKeySignature")));
    }

    public PreKeyBundle toPreKeyBundle() throws Exception {
        return new PreKeyBundle(
                registrationId, deviceId, preKeyId,
                new org.signal.libsignal.protocol.ecc.ECPublicKey(preKeyPublic),
                signedPreKeyId,
                new org.signal.libsignal.protocol.ecc.ECPublicKey(signedPreKeyPublic),
                signedPreKeySignature,
                new IdentityKey(identityKey, 0),
                kyberPreKeyId,
                new org.signal.libsignal.protocol.kem.KEMPublicKey(kyberPreKeyPublic),
                kyberPreKeySignature);
    }
}