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
    stdio: "ignore"
  });
  await wait(300);
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
    id:"msg-001",
    ciphertext:opaque
  }));

  const received = await nextMessage(bob);
  assert.equal(received.type, "ciphertext");
  assert.equal(received.ciphertext, opaque);
  assert.equal(received.id, "msg-001");
  assert.equal(typeof received.expiresAt, "number");

  alice.close();
  bob.close();
});
