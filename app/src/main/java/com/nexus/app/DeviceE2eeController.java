package com.nexus.app;

import android.content.Context;
import org.signal.libsignal.protocol.SessionBuilder;
import org.signal.libsignal.protocol.SessionCipher;
import org.signal.libsignal.protocol.SignalProtocolAddress;
import org.signal.libsignal.protocol.message.CiphertextMessage;
import org.signal.libsignal.protocol.message.PreKeySignalMessage;
import org.signal.libsignal.protocol.message.SignalMessage;
import org.signal.libsignal.protocol.ecc.ECKeyPair;
import org.signal.libsignal.protocol.kem.KEMKeyPair;
import org.signal.libsignal.protocol.kem.KEMKeyType;
import org.signal.libsignal.protocol.state.KyberPreKeyRecord;
import org.signal.libsignal.protocol.state.PreKeyBundle;
import org.signal.libsignal.protocol.state.PreKeyRecord;
import org.signal.libsignal.protocol.state.SignedPreKeyRecord;
import org.signal.libsignal.protocol.util.Medium;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;

/**
 * Device-facing E2EE session controller.
 * Only public pre-key material leaves the device. Message plaintext stays in-process.
 */
public final class DeviceE2eeController {
    public static final int PREKEY_TYPE = CiphertextMessage.PREKEY_TYPE;
    public static final int WHISPER_TYPE = CiphertextMessage.WHISPER_TYPE;

    public static final class CipherPacket {
        public final int type;
        public final byte[] bytes;
        CipherPacket(int type, byte[] bytes) {
            this.type = type;
            this.bytes = bytes;
        }
    }

    private final PersistentSignalProtocolStore store;
    private final SignalProtocolAddress localAddress;

    public DeviceE2eeController(Context context, String peerId, int deviceId) throws Exception {
        this.store = new PersistentSignalProtocolStore(context, "device-" + peerId);
        this.localAddress = new SignalProtocolAddress(peerId, deviceId);
    }

    public String localPeerId() {
        return localAddress.getName();
    }

    public int localDeviceId() {
        return localAddress.getDeviceId();
    }

    public SignalPreKeyEnvelope createLocalPreKeyBundle() throws Exception {
        SecureRandom random = new SecureRandom();
        int preKeyId = 1 + random.nextInt(Medium.MAX_VALUE - 1);
        int signedId = 1 + random.nextInt(Medium.MAX_VALUE - 1);
        int kyberId = 1 + random.nextInt(Medium.MAX_VALUE - 1);

        ECKeyPair preKey = ECKeyPair.generate();
        ECKeyPair signed = ECKeyPair.generate();
        byte[] signedSig = store.getIdentityKeyPair().getPrivateKey()
                .calculateSignature(signed.getPublicKey().serialize());

        KEMKeyPair kyber = KEMKeyPair.generate(KEMKeyType.KYBER_1024);
        byte[] kyberSig = store.getIdentityKeyPair().getPrivateKey()
                .calculateSignature(kyber.getPublicKey().serialize());

        store.storePreKey(preKeyId, new PreKeyRecord(preKeyId, preKey));
        store.storeSignedPreKey(signedId, new SignedPreKeyRecord(
                signedId, System.currentTimeMillis(), signed, signedSig));
        store.storeKyberPreKey(kyberId, new KyberPreKeyRecord(
                kyberId, System.currentTimeMillis(), kyber, kyberSig));

        return new SignalPreKeyEnvelope(
                store.getLocalRegistrationId(), localDeviceId(),
                preKeyId, preKey.getPublicKey().serialize(),
                signedId, signed.getPublicKey().serialize(), signedSig,
                store.getIdentityKeyPair().getPublicKey().serialize(),
                kyberId, kyber.getPublicKey().serialize(), kyberSig);
    }

    public void establishSession(String peerId, int deviceId, SignalPreKeyEnvelope envelope) throws Exception {
        SignalProtocolAddress remote = new SignalProtocolAddress(peerId, deviceId);
        PreKeyBundle bundle = envelope.toPreKeyBundle();
        new SessionBuilder(store, remote, localAddress).process(bundle);
    }

    public CipherPacket encrypt(String peerId, int deviceId, byte[] plaintext) throws Exception {
        SignalProtocolAddress remote = new SignalProtocolAddress(peerId, deviceId);
        CiphertextMessage encrypted = new SessionCipher(store, localAddress, remote).encrypt(plaintext);
        return new CipherPacket(encrypted.getType(), encrypted.serialize());
    }

    public byte[] decrypt(String peerId, int deviceId, int type, byte[] ciphertext) throws Exception {
        SignalProtocolAddress remote = new SignalProtocolAddress(peerId, deviceId);
        SessionCipher cipher = new SessionCipher(store, localAddress, remote);
        if (type == PREKEY_TYPE) return cipher.decrypt(new PreKeySignalMessage(ciphertext));
        if (type == WHISPER_TYPE) return cipher.decrypt(new SignalMessage(ciphertext));
        throw new IllegalArgumentException("Unsupported Signal ciphertext type: " + type);
    }
}