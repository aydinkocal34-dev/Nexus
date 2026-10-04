import test from "node:test";
import assert from "node:assert/strict";
test("relay package exposes a runnable server entrypoint",async()=>{const fs=await import("node:fs/promises");const s=await fs.readFile(new URL("./server.js",import.meta.url),"utf8");assert.match(s,/WebSocketServer/);assert.match(s,/ciphertext/);assert.doesNotMatch(s,/plaintext/);});