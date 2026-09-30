package com.nexus.app.security;

import org.junit.Test;
import static org.junit.Assert.*;

public class MessageExpiryTest {
    @Test public void expiresExactlySixtySecondsAfterRead() {
        assertEquals(160_000L, MessageExpiry.expiresAt(100_000L));
    }
    @Test public void doesNotExpireBeforeDeadline() {
        assertFalse(MessageExpiry.expired(159_999L, 160_000L));
    }
    @Test public void expiresAtDeadline() {
        assertTrue(MessageExpiry.expired(160_000L, 160_000L));
    }
    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeReadTime() {
        MessageExpiry.expiresAt(-1L);
    }
}
