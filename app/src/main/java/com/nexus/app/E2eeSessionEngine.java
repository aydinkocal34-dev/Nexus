package com.nexus.app;

import android.content.Context;
import org.signal.libsignal.protocol.IdentityKey;
import org.signal.libsignal.protocol.IdentityKeyPair;
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
import org.signal.libsignal.protocol.util.KeyHelper;
import org.signal.libsignal.protocol.util.Medium;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public final class E2eeSessionEngine {
    private static final SignalProtocolAddress ALICE = new SignalProtocolAddress("NX-LOCAL-ALICE", 1);
    private static final SignalProtocolAddress BOB = new SignalProtocolAddress("NX-LOCAL-BOB", 1);
    private E2eeSessionEngine() {}

    public static String runPersistentRoundTrip(Context context) throws Exception {
        PersistentSignalProtocolStore alice = new PersistentSignalProtocolStore(context, "smoke-alice");
        PersistentSignalProtocolStore bob = new PersistentSignalProtocolStore(context, "smoke-bob");
        alice.clearNamespace(); bob.clearNamespace();

        createBundle(bob);
        new SessionBuilder(alice, BOB, ALICE).process(createBundle(bob));
        SessionCipher a = new SessionCipher(alice, ALICE, BOB);
        SessionCipher b = new SessionCipher(bob, BOB, ALICE);

        byte[] first = "NEXUS PERSISTENT E2EE".getBytes(StandardCharsets.UTF_8);
        byte[] clear = decrypt(b, a.encrypt(first));
        if (!java.util.Arrays.equals(first, clear)) throw new IllegalStateException("First E2EE message failed");

        // Simulate process death: construct fresh store objects from the same encrypted state.
        alice = new PersistentSignalProtocolStore(context, "smoke-alice");
        bob = new PersistentSignalProtocolStore(context, "smoke-bob");
        a = new SessionCipher(alice, ALICE, BOB);
        b = new SessionCipher(bob, BOB, ALICE);

        byte[] second = "SESSION SURVIVED RESTART".getBytes(StandardCharsets.UTF_8);
        clear = decrypt(b, a.encrypt(second));
        if (!java.util.Arrays.equals(second, clear)) throw new IllegalStateException("Persistent session failed");

        return "PERSISTENT E2EE ACTIVE\nSession survived store reload\nKeystore AES-256-GCM + libsignal ratchet OK";
    }

    private static byte[] decrypt(SessionCipher receiver, CiphertextMessage encrypted) throws Exception {
        if (encrypted.getType() == CiphertextMessage.PREKEY_TYPE)
            return receiver.decrypt(new PreKeySignalMessage(encrypted.serialize()));
        if (encrypted.getType() == CiphertextMessage.WHISPER_TYPE)
            return receiver.decrypt(new SignalMessage(encrypted.serialize()));
        throw new IllegalStateException("Unknown ciphertext type: " + encrypted.getType());
    }

    private static PreKeyBundle createBundle(PersistentSignalProtocolStore device) throws Exception {
        // Reuse the existing active pre-key set when possible.
        ECKeyPair preKey = ECKeyPair.generate();
        ECKeyPair signedPreKey = ECKeyPair.generate();
        byte[] signedSignature = device.getIdentityKeyPair().getPrivateKey()
                .calculateSignature(signedPreKey.getPublicKey().serialize());
        KEMKeyPair kyber = KEMKeyPair.generate(KEMKeyType.KYBER_1024);
        byte[] kyberSignature = device.getIdentityKeyPair().getPrivateKey()
                .calculateSignature(kyber.getPublicKey().serialize());

        Random random = new Random();
        int preKeyId = random.nextInt(Medium.MAX_VALUE);
        int signedPreKeyId = random.nextInt(Medium.MAX_VALUE);
        int kyberPreKeyId = random.nextInt(Medium.MAX_VALUE);
        device.storePreKey(preKeyId, new PreKeyRecord(preKeyId, preKey));
        device.storeSignedPreKey(signedPreKeyId, new SignedPreKeyRecord(signedPreKeyId, System.currentTimeMillis(), signedPreKey, signedSignature));
        device.storeKyberPreKey(kyberPreKeyId, new KyberPreKeyRecord(kyberPreKeyId, System.currentTimeMillis(), kyber, kyberSignature));

        return new PreKeyBundle(device.getLocalRegistrationId(), 1, preKeyId, preKey.getPublicKey(),
                signedPreKeyId, signedPreKey.getPublicKey(), signedSignature,
                device.getIdentityKeyPair().getPublicKey(), kyberPreKeyId,
                kyber.getPublicKey(), kyberSignature);
    }
}