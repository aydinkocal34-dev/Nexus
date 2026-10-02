package com.nexus.app;

import org.junit.Test;
import org.signal.libsignal.protocol.CiphertextMessage;
import org.signal.libsignal.protocol.IdentityKey;
import org.signal.libsignal.protocol.IdentityKeyPair;
import org.signal.libsignal.protocol.SessionBuilder;
import org.signal.libsignal.protocol.SessionCipher;
import org.signal.libsignal.protocol.SignalProtocolAddress;
import org.signal.libsignal.protocol.ecc.ECKeyPair;
import org.signal.libsignal.protocol.kem.KEMKeyPair;
import org.signal.libsignal.protocol.kem.KEMKeyType;
import org.signal.libsignal.protocol.state.InMemorySignalProtocolStore;
import org.signal.libsignal.protocol.state.KyberPreKeyRecord;
import org.signal.libsignal.protocol.state.PreKeyBundle;
import org.signal.libsignal.protocol.state.PreKeyRecord;
import org.signal.libsignal.protocol.state.SignedPreKeyRecord;
import org.signal.libsignal.protocol.util.KeyHelper;
import org.signal.libsignal.protocol.util.Medium;
import java.util.Random;
import static org.junit.Assert.*;

public class RealE2eeSessionTest {
  private static final SignalProtocolAddress ALICE = new SignalProtocolAddress("NX-ALICE-TEST", 1);
  private static final SignalProtocolAddress BOB = new SignalProtocolAddress("NX-BOB-TEST", 1);

  private static final class Device {
    final IdentityKeyPair identity;
    final InMemorySignalProtocolStore store;
    Device() {
      ECKeyPair keys = ECKeyPair.generate();
      identity = new IdentityKeyPair(new IdentityKey(keys.getPublicKey()), keys.getPrivateKey());
      store = new InMemorySignalProtocolStore(identity, KeyHelper.generateRegistrationId(false));
    }
  }

  private static PreKeyBundle makeBundle(Device device) throws Exception {
    ECKeyPair preKey = ECKeyPair.generate();
    ECKeyPair signedPreKey = ECKeyPair.generate();
    byte[] signedSignature = device.identity.getPrivateKey().calculateSignature(signedPreKey.getPublicKey().serialize());
    KEMKeyPair kyber = KEMKeyPair.generate(KEMKeyType.KYBER_1024);
    byte[] kyberSignature = device.identity.getPrivateKey().calculateSignature(kyber.getPublicKey().serialize());
    Random random = new Random();
    int preKeyId = random.nextInt(Medium.MAX_VALUE);
    int signedPreKeyId = random.nextInt(Medium.MAX_VALUE);
    int kyberPreKeyId = random.nextInt(Medium.MAX_VALUE);
    device.store.storePreKey(preKeyId, new PreKeyRecord(preKeyId, preKey));
    device.store.storeSignedPreKey(signedPreKeyId, new SignedPreKeyRecord(signedPreKeyId, System.currentTimeMillis(), signedPreKey, signedSignature));
    device.store.storeKyberPreKey(kyberPreKeyId, new KyberPreKeyRecord(kyberPreKeyId, System.currentTimeMillis(), kyber, kyberSignature));
    return new PreKeyBundle(device.store.getLocalRegistrationId(), 1, preKeyId, preKey.getPublicKey(), signedPreKeyId, signedPreKey.getPublicKey(), signedSignature, device.identity.getPublicKey(), kyberPreKeyId, kyber.getPublicKey(), kyberSignature);
  }

  @Test
  public void aliceToBobAndBackUsesRealSignalSession() throws Exception {
    Device alice = new Device();
    Device bob = new Device();
    PreKeyBundle bobBundle = makeBundle(bob);
    new SessionBuilder(alice.store, BOB, ALICE).process(bobBundle);
    assertTrue(alice.store.containsSession(BOB));
    SessionCipher aliceCipher = new SessionCipher(alice.store, ALICE, BOB);
    byte[] first = "NEXUS gerçek E2EE test mesajı".getBytes("UTF-8");
    CiphertextMessage firstCiphertext = aliceCipher.encrypt(first);
    assertEquals(CiphertextMessage.PREKEY_TYPE, firstCiphertext.getType());
    SessionCipher bobCipher = new SessionCipher(bob.store, BOB, ALICE);
    assertArrayEquals(first, bobCipher.decrypt(firstCiphertext));
    assertTrue(bob.store.containsSession(ALICE));
    byte[] reply = "Bob'dan NEXUS cevabı".getBytes("UTF-8");
    CiphertextMessage replyCiphertext = bobCipher.encrypt(reply);
    assertEquals(CiphertextMessage.WHISPER_TYPE, replyCiphertext.getType());
    assertArrayEquals(reply, aliceCipher.decrypt(replyCiphertext));
    byte[] second = "Ratchet ikinci mesaj".getBytes("UTF-8");
    CiphertextMessage secondCiphertext = aliceCipher.encrypt(second);
    assertEquals(CiphertextMessage.WHISPER_TYPE, secondCiphertext.getType());
    assertArrayEquals(second, bobCipher.decrypt(secondCiphertext));
  }
}