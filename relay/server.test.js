const test=require("node:test");
const assert=require("node:assert/strict");
const {spawn}=require("node:child_process");
const http=require("node:http");
const WebSocket=require("ws");

const PORT=18080;
let child;

const wait=ms=>new Promise(r=>setTimeout(r,ms));
const connect=()=>new Promise((resolve,reject)=>{
  const ws=new WebSocket("ws://127.0.0.1:"+PORT);
  ws.once("open",()=>resolve(ws));
  ws.once("error",reject);
});
const next=ws=>new Promise((resolve,reject)=>{
  const onMessage=d=>{cleanup();try{resolve(JSON.parse(d.toString()));}catch(e){reject(e);}};
  const onError=e=>{cleanup();reject(e);};
  const cleanup=()=>{ws.off("message",onMessage);ws.off("error",onError);};
  ws.on("message",onMessage);ws.on("error",onError);
});

test.before(async()=>{
  child=spawn(process.execPath,["server.js"],{
    cwd:__dirname,
    env:{...process.env,PORT:String(PORT),MESSAGE_TTL_MS:"1000"},
    stdio:["ignore","pipe","pipe"]
  });
  let stderr="";
  child.stderr.on("data",d=>{stderr+=d.toString();});
  for(let i=0;i<100;i++){
    try{
      await new Promise((resolve,reject)=>{
        const req=http.get("http://127.0.0.1:"+PORT+"/health",res=>{
          res.resume();res.on("end",resolve);
        });
        req.on("error",reject);
        req.setTimeout(500,()=>{req.destroy();reject(new Error("health timeout"));});
      });
      return;
    }catch{await wait(100);}
  }
  throw new Error("relay server did not become ready"+(child.exitCode!==null?" (exit "+child.exitCode+"): "+stderr:""));
});
test.after(()=>child.kill());

test("health endpoint exposes no message content",async()=>{
  const body=await new Promise((resolve,reject)=>{
    http.get("http://127.0.0.1:"+PORT+"/health",res=>{
      let s="";res.on("data",d=>s+=d);res.on("end",()=>resolve(s));
    }).on("error",reject);
  });
  const p=JSON.parse(body);
  assert.equal(p.ok,true);
  assert.equal(p.service,"nexus-blind-relay");
  assert.equal("ciphertext" in p,false);
});

test("relay forwards opaque ciphertext",async()=>{
  const a=await connect(),b=await connect();
  a.send(JSON.stringify({type:"register",peerId:"alice-device-001"}));
  b.send(JSON.stringify({type:"register",peerId:"bob-device-001"}));
  await next(a);await next(b);
  const opaque=Buffer.from("NEXUS-CIPHERTEXT-BLOB").toString("base64");
  a.send(JSON.stringify({type:"relay",to:"bob-device-001",id:"msg-0001",ciphertext:opaque,ciphertextType:3,ttlMs:5000}));
  const r=await next(b);
  assert.equal(r.type,"ciphertext");
  assert.equal(r.ciphertext,opaque);
  assert.equal(r.ciphertextType,3);
  assert.equal(r.id,"msg-0001");
  assert.equal(typeof r.expiresAt,"number");
  a.close();b.close();
});

test("relay rejects duplicate peer registration",async()=>{
  const a=await connect(),b=await connect();
  a.send(JSON.stringify({type:"register",peerId:"duplicate-peer-001"}));
  assert.equal((await next(a)).type,"registered");
  b.send(JSON.stringify({type:"register",peerId:"duplicate-peer-001"}));
  assert.equal((await next(b)).code,"PEER_ALREADY_CONNECTED");
  a.close();b.close();
});

test("relay forwards opaque prekey envelope",async()=>{
  const a=await connect(),b=await connect();
  a.send(JSON.stringify({type:"register",peerId:"alice-prekey-001"}));
  b.send(JSON.stringify({type:"register",peerId:"bob-prekey-001"}));
  await next(a);await next(b);
  const bundle=Buffer.from(JSON.stringify({identity:"public-key-material",signedPreKey:"signed-public-key",preKey:"one-time-public-key"})).toString("base64");
  a.send(JSON.stringify({type:"prekey",to:"bob-prekey-001",id:"bundle-0001",bundle,ttlMs:5000}));
  const r=await next(b);
  assert.equal(r.type,"prekey");
  assert.equal(r.bundle,bundle);
  assert.equal(r.id,"bundle-0001");
  a.close();b.close();
});

test("read receipt is recipient-bound and ids cannot replay",async()=>{
  const a=await connect(),b=await connect(),m=await connect();
  a.send(JSON.stringify({type:"register",peerId:"alice-ack-001"}));
  b.send(JSON.stringify({type:"register",peerId:"bob-ack-001"}));
  m.send(JSON.stringify({type:"register",peerId:"mallory-001"}));
  await next(a);await next(b);await next(m);
  const opaque=Buffer.from("opaque-e2ee").toString("base64");
  a.send(JSON.stringify({type:"relay",to:"bob-ack-001",id:"msg-ack-0001",ciphertext:opaque,ciphertextType:3,ttlMs:5000}));
  assert.equal((await next(b)).type,"ciphertext");
  m.send(JSON.stringify({type:"ack",id:"msg-ack-0001",to:"alice-ack-001"}));
  assert.equal((await next(m)).code,"BAD_ACK");
  b.send(JSON.stringify({type:"ack",id:"msg-ack-0001"}));
  assert.equal((await next(a)).status,"read");
  a.send(JSON.stringify({type:"relay",to:"bob-ack-001",id:"msg-ack-0001",ciphertext:opaque,ciphertextType:3,ttlMs:5000}));
  assert.equal((await next(a)).code,"REPLAY_ID");
  a.close();b.close();m.close();
});
