import { WebSocketServer } from "ws";
import crypto from "node:crypto";
import http from "node:http";
const port=Number(process.env.PORT||8080);
const httpServer=http.createServer((req,res)=>{res.writeHead(200,{"content-type":"application/json"});res.end(JSON.stringify({service:"nexus-relay",status:"ok"}));});
const wss=new WebSocketServer({server:httpServer});
httpServer.listen(port,()=>console.log(`NEXUS relay listening on :${port}`));
const peers=new Map();
const pendingPreKeys=new Map();
const ttlMax=120000;
function send(ws,obj){if(ws.readyState===1)ws.send(JSON.stringify(obj));}
function id(){return crypto.randomUUID();}
function register(ws,peerId){
  if(typeof peerId!=="string"||peerId.length<8||peerId.length>128) return false;
  const old=peers.get(peerId); if(old&&old!==ws) old.close(4001,"replaced");
  peers.set(peerId,ws); ws.peerId=peerId; send(ws,{type:"registered",peerId});
  const queued=pendingPreKeys.get(peerId)||[]; pendingPreKeys.delete(peerId);
  for(const m of queued){ if(Date.now() < m.expiresAt) send(ws,m); }
  return true;
}
wss.on("connection",ws=>{
  console.log("NEXUS relay websocket connected");
  ws.on("message",raw=>{
    let m; try{m=JSON.parse(raw.toString())}catch{send(ws,{type:"error",code:"BAD_JSON"});return;}
    if(m.type==="register"){if(!register(ws,m.peerId))send(ws,{type:"error",code:"BAD_PEER_ID"});return;}
    if(!ws.peerId){send(ws,{type:"error",code:"NOT_REGISTERED"});return;}
    const target=typeof m.to==="string"?peers.get(m.to):null;
    const expiresAt=Date.now()+Math.min(Math.max(Number(m.ttlMs)||60000,1000),ttlMax);
    if(m.type==="prekey-request"||m.type==="prekey"){
      if(!target){
        if(m.type==="prekey-request"){
          const q=pendingPreKeys.get(m.to)||[];
          q.push({...m,from:ws.peerId,expiresAt});
          pendingPreKeys.set(m.to,q);
          send(ws,{type:"delivery",id:m.id,status:"queued"});
          return;
        }
        send(ws,{type:"delivery",id:m.id,status:"offline"});return;
      }
      console.log(`NEXUS relay ${m.type} ${ws.peerId} -> ${m.to}`);
      send(target,{...m,from:ws.peerId,expiresAt});send(ws,{type:"delivery",id:m.id,status:"relayed"});return;
    }
    if(m.type==="relay"){
      if(typeof m.ciphertext!=="string"||m.ciphertext.length>262144){send(ws,{type:"error",code:"BAD_CIPHERTEXT"});return;}
      if(!target){send(ws,{type:"delivery",id:m.id,status:"offline"});return;}
      send(target,{type:"ciphertext",id:m.id,from:ws.peerId,fromDeviceId:Number(m.fromDeviceId)||1,ciphertext:m.ciphertext,ciphertextType:Number(m.ciphertextType)||0,expiresAt});
      send(ws,{type:"delivery",id:m.id,status:"relayed"});return;
    }
    if(m.type==="ack"){
      if(target)send(target,{type:"delivery",id:m.id,status:"read"});
    }
  });
  ws.on("close",()=>{console.log(`NEXUS relay websocket closed ${ws.peerId||""}`);if(ws.peerId&&peers.get(ws.peerId)===ws)peers.delete(ws.peerId);});
});
console.log(`NEXUS relay listening on :${port}`);