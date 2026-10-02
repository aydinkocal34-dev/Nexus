# NEXUS Blind Relay

The relay transports opaque ciphertext only.

## Protocol

- Client connects over WebSocket.
- Client sends `register` with an opaque device identifier.
- Client sends `relay` containing `to`, message `id`, and base64 ciphertext.
- Relay forwards ciphertext without decrypting, indexing plaintext, or logging message content.
- Recipient can send an `ack` to signal a read/delivery event.
- Offline ciphertext is not queued by this first relay implementation.
- Each forwarded packet receives a short server expiry timestamp.

## Run

```bash
npm install
npm test
PORT=8080 npm start
```

Production deployment must put the WebSocket endpoint behind TLS (wss://), authentication, durable abuse controls, monitoring, and an independently reviewed protocol implementation.
