package com.nexus.app;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class LocalTwoDeviceE2eeTest {
    @Test
    public void twoIndependentDeviceIdentitiesEncryptAndDecryptBothWays() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        String a = "NX-TEST-A-" + System.nanoTime();
        String b = "NX-TEST-B-" + System.nanoTime();

        DeviceE2eeController alice = new DeviceE2eeController(context, a, 1);
        DeviceE2eeController bob = new DeviceE2eeController(context, b, 1);

        SignalPreKeyEnvelope bobBundle = bob.createLocalPreKeyBundle();
        alice.establishSession(b, 1, bobBundle);

        byte[] first = "NEXUS ARM64 gerçek E2EE testi".getBytes(StandardCharsets.UTF_8);
        DeviceE2eeController.CipherPacket aToB = alice.encrypt(b, 1, first);
        assertArrayEquals(first, bob.decrypt(a, 1, aToB.type, aToB.bytes));

        byte[] reply = "NEXUS B yönünden de şifreli".getBytes(StandardCharsets.UTF_8);
        DeviceE2eeController.CipherPacket bToA = bob.encrypt(a, 1, reply);
        assertArrayEquals(reply, alice.decrypt(b, 1, bToA.type, bToA.bytes));

        DeviceE2eeController bobAfterRestart = new DeviceE2eeController(context, b, 1);
        byte[] afterRestart = "Oturum kalıcılığı testi".getBytes(StandardCharsets.UTF_8);
        DeviceE2eeController.CipherPacket next = alice.encrypt(b, 1, afterRestart);
        assertArrayEquals(afterRestart, bobAfterRestart.decrypt(a, 1, next.type, next.bytes));

        assertNotEquals(alice.localPeerId(), bob.localPeerId());
    }
}
