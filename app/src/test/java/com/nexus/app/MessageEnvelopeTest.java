package com.nexus.app;

import org.junit.Test;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class MessageEnvelopeTest {
    @Test
    public void envelopeHasSixtySecondExpiryAndOpaqueCiphertext() {
        byte[] ciphertext = "opaque-signal-ciphertext".getBytes(StandardCharsets.UTF_8);
        MessageEnvelope e = MessageEnvelope.create(ciphertext, 3, 1_000_000L);
        assertEquals(60_000L, e.expiresAtMs - e.createdAtMs);
        assertFalse(e.isExpired(1_059_999L));
        assertTrue(e.isExpired(1_060_000L));
        assertArrayEquals(ciphertext, e.copyCiphertext());
    }

    @Test(expected = IllegalArgumentException.class)
    public void expiredEnvelopeCannotBeCreated() {
        new MessageEnvelope("1234567890123456", 1000, 999, 3, new byte[]{1});
    }
}