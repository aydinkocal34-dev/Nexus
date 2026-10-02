const test = require("node:test");
const assert = require("node:assert/strict");
const { spawn } = require("node:child_process");
const http = require("http");
const WebSocket = require("ws");

const PORT = 18080;
let child;

function wait(ms) {
  return new Promise(r => setTimeout(r, ms));
}

function connect() {
  return new Promise((resolve, reject) => {
    const ws = new WebSocket("ws://127.0.0.1:" + PORT);
    ws.once("open", () => resolve(ws));
    ws.once("error", reject);
  });
}

function nextMessage(ws) {
  return new Promise((resolve, reject) => {
    const onMessage = data => {
      cleanup();
      try { resolve(JSON.parse(data.toString())); } catch (e) { reject(e); }
    };
    const onError = e => { cleanup(); reject(e); };
    const cleanup = () => {
      ws.off("message", onMessage);
      ws.off("error", onError);
    };
    ws.on("message", onMessage);
    ws.on("error", onError);
  });
}

test.before(async () => {
  child = spawn(process.execPath, ["server.js"], {
    cwd: __dirname,
    env: { ...process.env, PORT: String(PORT), MESSAGE_TTL_MS: "1000" },
    stdio: ["ignore", "pipe", "pipe"]
  });
  let stderr = "";
  child.stderr.on("data", d => { stderr += d.toString(); });\n  for (let i = 0; i < 100; i++) {
    try {
      await new Promise((resolve, reject) => {
        const req = http.get("http://127.0.0.1:" + PORT + "/health", res => {
          res.resume();
          res.on("end", () => resolve());
        });
        req.on("error", reject);
        req.setTimeout(500, () => { req.destroy(); reject(new Error("health timeout")); });
      });
      return;
    } catch { await wait(100); }
  }
  throw new Error("relay server did not become ready" + (child.exitCode !== null ? " (exit " + child.exitCode + "): " + stderr : "") );
});

test.after(() => child.kill());

test("health endpoint exposes no message content", async () => {
  const body = await new Promise((resolve, reject) => {
    http.get("http://127.0.0.1:" + PORT + "/health", res => {
      let s = "";
      res.on("data", d => s += d);
      res.on("end", () => resolve(s));
    }).on("error", reject);
  });
  const parsed = JSON.parse(body);
  assert.equal(parsed.ok, true);
  assert.equal(parsed.service, "nexus-blind-relay");
  assert.equal("ciphertext" in parsed, false);
});

test("relay forwards opaque ciphertext and does not require plaintext", async () => {
  const alice = await connect();
  const bob = await connect();

  alice.send(JSON.stringify({type:"register", peerId:"alice-device-001"}));
  bob.send(JSON.stringify({type:"register", peerId:"bob-device-001"}));
  await nextMessage(alice);
  await nextMessage(bob);

  const opaque = Buffer.from("NEXUS-CIPHERTEXT-BLOB").toString("base64");
  alice.send(JSON.stringify({
    type:"relay",
    to:"bob-device-001",
    id:"msg-0001",
    ciphertext:opaque,
    ciphertextType:3,
    ttlMs:5000
  }));

  const received = await nextMessage(bob);
  assert.equal(received.type, "ciphertext");
  assert.equal(received.ciphertext, opaque);
  assert.equal(received.ciphertextType, 3);
  assert.equal(received.id, "msg-0001");
  assert.equal(typeof received.expiresAt, "number");

  alice.close();
  bob.close();
});


test("relay forwards opaque prekey envelopes without parsing them", async () => {
  const alice = await connect();
  const bob = await connect();

  alice.send(JSON.stringify({type:"register", peerId:"alice-prekey-001"}));
  bob.send(JSON.stringify({type:"register", peerId:"bob-prekey-001"}));
  await nextMessage(alice);
  await nextMessage(bob);

  const bundle = Buffer.from(JSON.stringify({
    identity:"public-key-material",
    signedPreKey:"signed-public-key",
    preKey:"one-time-public-key"
  })).toString("base64");

  alice.send(JSON.stringify({
    type:"prekey",
    to:"bob-prekey-001",
    id:"bundle-0001",
    bundle,
    ttlMs:5000
  }));

  const received = await nextMessage(bob);
  assert.equal(received.type, "prekey");
  assert.equal(received.bundle, bundle);
  assert.equal(received.id, "bundle-0001");
  assert.equal(typeof received.expiresAt, "number");

  alice.close();
  bob.close();
});


test("read receipt is bound to the delivered recipient and message id cannot be replayed", async () => {
  const alice = await connect();
  const bob = await connect();
  const mallory = await connect();

  alice.send(JSON.stringify({type:"register", peerId:"alice-ack-001"}));
  bob.send(JSON.stringify({type:"register", peerId:"bob-ack-001"}));
  mallory.send(JSON.stringify({type:"register", peerId:"mallory-001"}));
  await nextMessage(alice);
  await nextMessage(bob);
  await nextMessage(mallory);

  const opaque = Buffer.from("opaque-e2ee").toString("base64");
  alice.send(JSON.stringify({
    type:"relay", to:"bob-ack-001", id:"msg-ack-0001",
    ciphertext:opaque, ciphertextType:3, ttlMs:5000
  }));
  const received = await nextMessage(bob);
  assert.equal(received.type, "ciphertext");

  mallory.send(JSON.stringify({type:"ack", id:"msg-ack-0001", to:"alice-ack-001"}));
  const badAck = await nextMessage(mallory);
  assert.equal(badAck.code, "BAD_ACK");

  bob.send(JSON.stringify({type:"ack", id:"msg-ack-0001"}));
  const read = await nextMessage(alice);
  assert.equal(read.status, "read");

  alice.send(JSON.stringify({
    type:"relay", to:"bob-ack-001", id:"msg-ack-001",
    ciphertext:opaque, ciphertextType:3, ttlMs:5000
  }));
  const replay = await nextMessage(alice);
  assert.equal(replay.code, "REPLAY_ID");

  alice.close();
  bob.close();
  mallory.close();
});
