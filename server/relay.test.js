import test from "node:test";
import assert from "node:assert/strict";
import {spawn} from "node:child_process";
import {WebSocket} from "ws";

const wait=ms=>new Promise(r=>setTimeout(r,ms));
async function waitFor(arr,predicate,label,timeout=8000){
  const end=Date.now()+timeout;
  while(Date.now()<end){
    const x=arr.find(predicate);
    if(x)return x;
    await wait(25);
  }
  throw new Error("timeout waiting for "+label+"; got "+JSON.stringify(arr));
}
function client(url,peerId){
  return new Promise((resolve,reject)=>{
    const ws=new WebSocket(url),messages=[];
    const timer=setTimeout(()=>{try{ws.close()}catch{};reject(new Error(peerId+" connect timeout"));},5000);
    ws.on("open",()=>{
      clearTimeout(timer);
      ws.on("message",b=>{try{messages.push(JSON.parse(b.toString()))}catch{}});
      ws.send(JSON.stringify({type:"register",peerId}));
      resolve({ws,messages});
    });
    ws.on("error",reject);
  });
}

test("relay queues encrypted ciphertext while recipient is offline and delivers after reconnect",async()=>{
  const port=18080+Math.floor(Math.random()*1000);
  const server=spawn(process.execPath,["server.js"],{cwd:new URL(".",import.meta.url),env:{...process.env,PORT:String(port)},stdio:["ignore","pipe","pipe"]});
  const url="ws://127.0.0.1:"+port;
  try{
    await new Promise((resolve,reject)=>{
      const timer=setTimeout(()=>reject(new Error("server start timeout")),5000);
      server.stdout.on("data",d=>{if(String(d).includes("NEXUS relay listening")){clearTimeout(timer);resolve();}});
      server.stderr.on("data",d=>process.stderr.write(d));
      server.on("error",reject);
    });

    const A=await client(url,"test-peer-A");
    let B=await client(url,"test-peer-B");
    await waitFor(A.messages,m=>m.type==="registered","A registered");
    await waitFor(B.messages,m=>m.type==="registered","B registered");

    B.ws.close(); await wait(100);

    const ids=Array.from({length:10},(_,i)=>"offline-"+i+"-"+Date.now());
    for(const id of ids){
      A.ws.send(JSON.stringify({
        type:"relay",id,to:"test-peer-B",
        ciphertext:Buffer.from("opaque-"+id).toString("base64"),
        ciphertextType:1,fromDeviceId:1,ttlMs:86400000
      }));
    }
    for(const id of ids) await waitFor(A.messages,m=>m.type==="delivery"&&m.id===id&&m.status==="queued","queued "+id);

    B=await client(url,"test-peer-B");
    await waitFor(B.messages,m=>m.type==="registered","B re-registered");
    for(const id of ids) await waitFor(B.messages,m=>m.type==="ciphertext"&&m.id===id,"ciphertext "+id);

    for(const id of ids){
      B.ws.send(JSON.stringify({type:"received",id,to:"test-peer-A"}));
      await waitFor(A.messages,m=>m.type==="delivery"&&m.id===id&&m.status==="delivered","delivered "+id);
    }

    const duplicate=ids[0];
    const before=B.messages.filter(m=>m.type==="ciphertext"&&m.id===duplicate).length;
    A.ws.send(JSON.stringify({
      type:"relay",id:duplicate,to:"test-peer-B",
      ciphertext:Buffer.from("opaque-"+duplicate).toString("base64"),
      ciphertextType:1,fromDeviceId:1,ttlMs:86400000
    }));
    await waitFor(A.messages,m=>m.type==="delivery"&&m.id===duplicate&&m.status==="delivered","duplicate delivery");
    await wait(150);
    const after=B.messages.filter(m=>m.type==="ciphertext"&&m.id===duplicate).length;
    assert.equal(after,before);

    A.ws.close();B.ws.close();
  }finally{
    server.kill("SIGTERM");
  }
});