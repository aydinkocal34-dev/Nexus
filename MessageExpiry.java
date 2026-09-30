package com.nexus.app.security;

public final class MessageExpiry {
    private MessageExpiry() {}
    public static long expiresAt(long readAtMillis) {
        if (readAtMillis < 0) throw new IllegalArgumentException("readAtMillis");
        return Math.addExact(readAtMillis, 60_000L);
    }
    public static boolean expired(long nowMillis, long expiresAtMillis) {
        return nowMillis >= expiresAtMillis;
    }
}
