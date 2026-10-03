package com.nexus.app;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import static org.junit.Assert.*;

public class LocalTwoDeviceE2eeTest {
    @Test
    public void twoIndependentDeviceIdentitiesEncryptAndDecrypt() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        String a = "NX-TEST-A-" + System.nanoTime();
        String b = "NX-TEST-B-" + System.nanoTime();

        DeviceE2eeController alice = new DeviceE2eeController(context, a, 1);
        DeviceE2eeController bob = new DeviceE2eeController(context, b, 1);

        SignalPreKeyEnvelope bobBundle = bob.createLocalPreKeyBundle();
        alice.establishSession(b, 1, bobBundle);

        byte[] plaintext = "NEXUS ARM64 gerçek E2EE testi".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        DeviceE2eeController.CipherPacket packet = alice.encrypt(b, 1, plaintext);
        byte[] clear = bob.decrypt(a, 1, packet.type, packet.bytes);

        assertArrayEquals(plaintext, clear);
        assertNotEquals(alice.localPeerId(), bob.localPeerId());
    }
}
