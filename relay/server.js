const http = require("http");
const { WebSocketServer } = require("ws");
const crypto = require("crypto");

const PORT = Number(process.env.PORT || 8080);
const MAX_FRAME = Number(process.env.MAX_FRAME_BYTES || 65536);
const TTL_MS = Number(process.env.MESSAGE_TTL_MS || 120000);
const RATE_LIMIT = Number(process.env.RATE_LIMIT_PER_MINUTE || 120);

const peers = new Map();
const buckets = new Map();
const deliveries = new Map();
const seenIds = new Map();

function token() {
  return crypto.randomBytes(16).toString("hex");
}

function allowed(ws) {
  const now = Date.now();
  let b = buckets.get(ws);
  if (!b || now - b.started >= 60000) {
    b = { started: now, count: 0 };
    buckets.set(ws, b);
  }
  b.count++;
  return b.count <= RATE_LIMIT;
}

function send(ws, obj) {
  if (ws.readyState === ws.OPEN) ws.send(JSON.stringify(obj));
}

function health(req, res) {
  const body = JSON.stringify({
    ok: true,
    service: "nexus-blind-relay",
    protocol: 1,
    peers: peers.size,
    timestamp: Date.now()
  });
  res.writeHead(200, {
    "content-type": "application/json",
    "cache-control": "no-store"
  });
  res.end(body);
}

const httpServer = http.createServer((req, res) => {
  if (req.method === "GET" && req.url === "/health") return health(req, res);
  res.writeHead(404);
  res.end();
});

const wss = new WebSocketServer({
  server: httpServer,
  maxPayload: MAX_FRAME
});

wss.on("connection", (ws) => {
  ws.id = token();
  ws.peerId = null;

  ws.on("message", (raw) => {
    if (!allowed(ws)) {
      send(ws, { type: "error", code: "RATE_LIMIT" });
      ws.close(1008, "rate limit");
      return;
    }

    let msg;
    try {
      msg = JSON.parse(raw.toString("utf8"));
    } catch {
      send(ws, { type: "error", code: "BAD_FRAME" });
      return;
    }

    if (msg.type === "register") {
      if (typeof msg.peerId !== "string" || msg.peerId.length < 8 || msg.peerId.length > 128) {
        send(ws, { type: "error", code: "BAD_PEER_ID" });
        return;
      }
      ws.peerId = msg.peerId;
      peers.set(msg.peerId, ws);
      send(ws, { type: "registered", peerId: msg.peerId });
      return;
    }

    if (msg.type === "relay") {
      if (!ws.peerId || typeof msg.to !== "string" ||
          typeof msg.id !== "string" || typeof msg.ciphertext !== "string") {
        send(ws, { type: "error", code: "BAD_RELAY" });
        return;
      }

      if (msg.id.length < 8 || msg.id.length > 128 || msg.ciphertext.length > MAX_FRAME) {
        send(ws, { type: "error", code: "BAD_RELAY" });
        return;
      }

      const recipient = peers.get(msg.to);
      if (!recipient) {
        send(ws, { type: "delivery", id: msg.id, status: "offline" });
        return;
      }

      const packet = {
        type: "ciphertext",
        id: msg.id,
        from: ws.peerId,
        ciphertext: msg.ciphertext,
        ciphertextType: Number.isInteger(msg.ciphertextType) ? msg.ciphertextType : 0,
        expiresAt: Date.now() + Math.min(TTL_MS, Math.max(1000, Number(msg.ttlMs) || TTL_MS))
      };

      // Blind relay: packet.ciphertext is opaque and is never parsed or logged.
      if (deliveries.has(msg.id)) {
        send(ws, { type: "error", code: "REPLAY_ID" });
        return;
      }
      seenIds.set(msg.id, packet.expiresAt);\n      deliveries.set(msg.id, { sender: ws.peerId, recipient: msg.to, expiresAt: packet.expiresAt });
      send(recipient, packet);
      send(ws, { type: "delivery", id: msg.id, status: "sent" });
      return;
    }

    if (msg.type === "prekey") {
      if (!ws.peerId || typeof msg.to !== "string" || typeof msg.id !== "string" ||
          typeof msg.bundle !== "string") {
        send(ws, { type: "error", code: "BAD_PREKEY" });
        return;
      }

      const recipient = peers.get(msg.to);
      if (!recipient) {
        send(ws, { type: "delivery", id: msg.id, status: "offline" });
        return;
      }

      // Public Signal pre-key material is forwarded as an opaque envelope.
      send(recipient, {
        type: "prekey",
        id: msg.id,
        from: ws.peerId,
        bundle: msg.bundle,
        expiresAt: Date.now() + Math.min(TTL_MS, Math.max(1000, Number(msg.ttlMs) || TTL_MS))
      });
      send(ws, { type: "delivery", id: msg.id, status: "sent" });
      return;
    }

    if (msg.type === "ack") {
      if (!ws.peerId || typeof msg.id !== "string") return;
      const delivery = deliveries.get(msg.id);
      if (!delivery || delivery.recipient !== ws.peerId || delivery.expiresAt < Date.now()) {
        send(ws, { type: "error", code: "BAD_ACK" });
        return;
      }
      const sender = peers.get(delivery.sender);
      if (sender) send(sender, { type: "delivery", id: msg.id, status: "read" });
      deliveries.delete(msg.id);
    }
  });

  ws.on("close", () => {
    if (ws.peerId && peers.get(ws.peerId) === ws) peers.delete(ws.peerId);
    buckets.delete(ws);
  });
});

setInterval(() => {
  const now = Date.now();
  for (const [id, delivery] of deliveries) {
    if (delivery.expiresAt <= now) deliveries.delete(id);
  }
}, Math.max(1000, TTL_MS)).unref();

httpServer.listen(PORT, "0.0.0.0", () => {
  console.log("NEXUS blind relay listening on " + PORT);
});
