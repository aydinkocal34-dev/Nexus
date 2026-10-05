import { WebSocketServer } from "ws";
import http from "node:http";

const port = Number(process.env.PORT || 8080);
const httpServer = http.createServer((req,res) => {
  res.writeHead(200, {"content-type":"application/json"});
  res.end(JSON.stringify({service:"nexus-relay",status:"ok"}));
});
const wss = new WebSocketServer({server:httpServer});
httpServer.listen(port, () => console.log(`NEXUS relay listening on :${port}`));

const peers = new Map();
const pendingPreKeys = new Map();
const pendingCiphertexts = new Map();
const deliveredRelayIds = new Map();

const preKeyTtlMax = 120000;
const messageTtlMax = 24 * 60 * 60 * 1000;
const maxQueuedMessagesPerPeer = 500;
const maxQueuedBytesPerPeer = 5 * 1024 * 1024;
const deliveredTtl = 24 * 60 * 60 * 1000;

function send(ws,obj) {
  if (ws && ws.readyState === 1) return ws.send(JSON.stringify(obj));
  return false;
}

function queueKey(from,to,id) { return `${from}|\${to}|${id}`; }

function pruneDelivered() {
  const now = Date.now();
  for (const [key,expiresAt] of deliveredRelayIds) {
    if (expiresAt <= now) deliveredRelayIds.delete(key);
  }
}

function enqueueCiphertext(to, message) {
  const q = pendingCiphertexts.get(to) || [];
  const bytes = Buffer.byteLength(message.ciphertext, "utf8");
  const key = queueKey(message.from, to, message.id);
  if (q.some(x => queueKey(x.from,to,x.id) === key)) return true;

  let total = q.reduce((sum,x) => sum + Buffer.byteLength(x.ciphertext, "utf8"), 0);
  while (q.length >= maxQueuedMessagesPerPeer || total + bytes > maxQueuedBytesPerPeer) {
    const removed = q.shift();
    if (!removed) break;
    total -= Buffer.byteLength(removed.ciphertext, "utf8");
  }
  if (bytes > maxQueuedBytesPerPeer) return false;
  q.push(message);
  pendingCiphertexts.set(to,q);
  return true;
}

function flushCiphertexts(peerId, ws) {
  const q = pendingCiphertexts.get(peerId);
  if (!q) return;
  const now = Date.now();
  const keep = [];
  for (const m of q) {
    if (m.expiresAt <= now) continue;
    send(ws,m);
    keep.push(m);
  }
  if (keep.length) pendingCiphertexts.set(peerId,keep);
  else pendingCiphertexts.delete(peerId);
}

function markDelivered(from,to,id) {
  const key = queueKey(from,to,id);
  deliveredRelayIds.set(key,Date.now()+deliveredTtl);
  const q = pendingCiphertexts.get(to);
  if (!q) return;
  const keep = q.filter(m => queueKey(m.from,to,m.id) !== key);
  if (keep.length) pendingCiphertexts.set(to,keep);
  else pendingCiphertexts.delete(to);
}

function register(ws,peerId) {
  if (typeof peerId !== "string" || peerId.length < 8 || peerId.length > 128) return false;
  const old = peers.get(peerId);
  if (old && old !== ws) old.close(4001,"replaced");
  peers.set(peerId,ws);
  ws.peerId=peerId;
  send(ws,{type:"registered",peerId});

  const queuedPreKeys = pendingPreKeys.get(peerId) || [];
  pendingPreKeys.delete(peerId);
  for (const m of queuedPreKeys) {
    if (Date.now() < m.expiresAt) send(ws,m);
  }
  flushCiphertexts(peerId,ws);
  return true;
}

setInterval(() => {
  const now=Date.now();
  for (const [peer,q] of pendingPreKeys) {
    const keep=q.filter(m=>m.expiresAt>now);
    if (keep.length) pendingPreKeys.set(peer,keep); else pendingPreKeys.delete(peer);
  }
  for (const [peer,q] of pendingCiphertexts) {
    const keep=q.filter(m=>m.expiresAt>now);
    if (keep.length) pendingCiphertexts.set(peer,keep); else pendingCiphertexts.delete(peer);
  }
  pruneDelivered();
},60000).unref();

wss.on("connection",ws=>{
  console.log("NEXUS relay websocket connected");
  ws.on("message",raw=>{
    let m;
    try { m=JSON.parse(raw.toString()); }
    catch { send(ws,{type:"error",code:"BAD_JSON"}); return; }

    if (m.type==="register") {
      if (!register(ws,m.peerId)) send(ws,{type:"error",code:"BAD_PEER_ID"});
      return;
    }
    if (!ws.peerId) { send(ws,{type:"error",code:"NOT_REGISTERED"}); return; }

    if (m.type==="received") {
      const from = typeof m.to==="string" ? m.to : "";
      if (!from || typeof m.id!=="string") return;
      markDelivered(from,ws.peerId,m.id);
      const sender=peers.get(from);
      if (sender) send(sender,{type:"delivery",id:m.id,status:"delivered"});
      return;
    }

    const target=typeof m.to==="string" ? peers.get(m.to) : null;

    if (m.type==="prekey-request" || m.type==="prekey") {
      const expiresAt=Date.now()+Math.min(Math.max(Number(m.ttlMs)||60000,1000),preKeyTtlMax);
      if (!target) {
        const q=pendingPreKeys.get(m.to)||[];
        q.push({...m,from:ws.peerId,expiresAt});
        pendingPreKeys.set(m.to,q);
        send(ws,{type:"delivery",id:m.id,status:"queued"});
        return;
      }
      send(target,{...m,from:ws.peerId,expiresAt});
      send(ws,{type:"delivery",id:m.id,status:"relayed"});
      return;
    }

    if (m.type==="relay") {
      if (typeof m.id!=="string" || m.id.length<8 || m.id.length>128) {
        send(ws,{type:"error",code:"BAD_MESSAGE_ID"}); return;
      }
      if (typeof m.ciphertext!=="string" || m.ciphertext.length>262144) {
        send(ws,{type:"error",code:"BAD_CIPHERTEXT"}); return;
      }

      pruneDelivered();
      const key=queueKey(ws.peerId,m.to,m.id);
      if (deliveredRelayIds.has(key)) {
        send(ws,{type:"delivery",id:m.id,status:"delivered"});
        return;
      }

      const expiresAt=Date.now()+Math.min(Math.max(Number(m.ttlMs)||86400000,1000),messageTtlMax);
      const envelope={
        type:"ciphertext", id:m.id, from:ws.peerId,
        fromDeviceId:Number(m.fromDeviceId)||1,
        ciphertext:m.ciphertext,
        ciphertextType:Number(m.ciphertextType)||0,
        expiresAt
      };
      if (!enqueueCiphertext(m.to,envelope)) {
        send(ws,{type:"delivery",id:m.id,status:"queue_full"});
        return;
      }

      send(ws,{type:"delivery",id:m.id,status:"queued"});
      if (target) send(target,envelope);
      return;
    }

    if (m.type==="ack") {
      if (target) send(target,{type:"delivery",id:m.id,status:"read"});
    }
  });

  ws.on("close",()=>{
    console.log(`NEXUS relay websocket closed ${ws.peerId||""}`);
    if (ws.peerId && peers.get(ws.peerId)===ws) peers.delete(ws.peerId);
  });
});

console.log(`NEXUS relay listening on :${port}`);