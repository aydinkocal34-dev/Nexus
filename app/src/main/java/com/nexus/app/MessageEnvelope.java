package com.nexus.app;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Authenticated transport metadata kept separate from plaintext.
 * Plaintext is encrypted by libsignal before this envelope is created.
 */
public final class MessageEnvelope {
    public final String id;
    public final long createdAtMs;
    public final long expiresAtMs;
    public final int ciphertextType;
    public final byte[] ciphertext;

    public MessageEnvelope(String id, long createdAtMs, long expiresAtMs,
                           int ciphertextType, byte[] ciphertext) {
        if (id == null || id.length() < 16 || id.length() > 128) {
            throw new IllegalArgumentException("invalid message id");
        }
        if (ciphertext == null || ciphertext.length == 0) {
            throw new IllegalArgumentException("empty ciphertext");
        }
        if (expiresAtMs <= createdAtMs) {
            throw new IllegalArgumentException("invalid expiry");
        }
        this.id = id;
        this.createdAtMs = createdAtMs;
        this.expiresAtMs = expiresAtMs;
        this.ciphertextType = ciphertextType;
        this.ciphertext = ciphertext.clone();
    }

    public static MessageEnvelope create(byte[] ciphertext, int type, long nowMs) {
        return new MessageEnvelope(
            UUID.randomUUID().toString(),
            nowMs,
            nowMs + 60_000L,
            type,
            ciphertext
        );
    }

    public boolean isExpired(long nowMs) {
        return nowMs >= expiresAtMs;
    }

    public String encodeCiphertext() {
        return Base64.getEncoder().encodeToString(ciphertext);
    }

    public byte[] copyCiphertext() {
        return ciphertext.clone();
    }
}