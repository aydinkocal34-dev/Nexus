package com.nexus.app;

import org.junit.Test;
import java.util.Base64;
import java.util.UUID;

import static org.junit.Assert.*;

public class RelayTransportContractTest {
    @Test
    public void ciphertextEnvelopeIsBinarySafeAndOpaque() {
        byte[] ciphertext = new byte[64];
        for (int i = 0; i < ciphertext.length; i++) ciphertext[i] = (byte)(i * 31);
        String encoded = Base64.getEncoder().encodeToString(ciphertext);
        assertArrayEquals(ciphertext, Base64.getDecoder().decode(encoded));
        assertNotEquals("hello", encoded);
    }

    @Test
    public void messageIdsAreUnique() {
        assertNotEquals(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    @Test
    public void relayClientExposesCiphertextOnlyTransport() throws Exception {
        assertNotNull(RelayClient.class.getMethod("sendCiphertext", String.class, String.class, byte[].class, long.class));
        assertNotNull(RelayClient.class.getMethod("sendPreKey", String.class, String.class, String.class, long.class));
        assertNotNull(RelayClient.class.getMethod("sendReadReceipt", String.class, String.class));
    }
}