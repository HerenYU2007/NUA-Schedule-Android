// One bounded read per node for diagnostics. No authentication or script execution.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const root=path.resolve(__dirname,'..');
const java=fs.readFileSync(path.join(root,'app/src/main/java/cn/nua/wenguan/schedule/UpdateMirrorPolicy.java'),'utf8');
const builtin=[...java.slice(java.indexOf('BUILTIN={'),java.indexOf('private UpdateMirrorPolicy')).matchAll(/"(https:\/\/[^"]+)"/g)].map(m=>new URL(m[1]).href);
const pub=fs.readFileSync(path.join(root,'app/src/main/assets/update-public.pem'));
async function bytes(url,max=1048576){
 const r=await fetch(url,{signal:AbortSignal.timeout(20000),headers:{'User-Agent':'NUA-Schedule-Android','Accept':'application/octet-stream','Cache-Control':'no-cache'}});
 if(!r.ok||new URL(r.url).protocol!=='https:')throw Error('HTTP/HTTPS');
 let n=0,blocks=[];for await(const b of r.body){n+=b.length;if(n>max)throw Error('limit');blocks.push(b);}return Buffer.concat(blocks);
}
function extract(s){return [...s.matchAll(/label\s*:\s*["'](?:search|contribute)["']\s*,\s*value\s*:\s*["']([^"'\s]+)["']/g)].map(m=>new URL(m[1].startsWith('https://')?m[1]:'https://'+m[1]+'/').href);}
(async()=>{
 let dynamic=[];try{
  const page=(await bytes('https://github.akams.cn/')).toString('utf8');dynamic.push(...extract(page));
  const chunks=[...new Set([...page.matchAll(/\/_next\/static\/chunks\/[A-Za-z0-9_.-]+\.js/g)].map(m=>m[0]))].slice(0,24);
  await Promise.all(chunks.map(async p=>{try{dynamic.push(...extract((await bytes('https://github.akams.cn'+p)).toString('utf8')));}catch{}}));
 }catch{}
 const nodes=[...new Set(['https://gh-proxy.org/',...dynamic,...builtin])];
 const original='https://github.com/HerenYU2007/NUA-Schedule-Android/releases/latest/download/update.json';
 const jobs=[...nodes.map(base=>({base,url:base+original})),{base:'https://github.com/',url:original}];let index=0;const results=[];
 async function worker(){for(;;){const i=index++;if(i>=jobs.length)return;const job=jobs[i],start=Date.now();try{
   const envelope=JSON.parse((await bytes(job.url,131072)).toString('utf8')),payload=Buffer.from(envelope.payload,'base64');
   if(envelope.algorithm!=='SHA256withRSA'||!crypto.verify('sha256',payload,pub,Buffer.from(envelope.signature,'base64')))throw Error('signature');
   const release=JSON.parse(payload);if(release.packageName!=='cn.nua.wenguan.schedule')throw Error('package');
   results.push({host:new URL(job.base).host,ok:true,version:release.versionName,ms:Date.now()-start});
 }catch(error){results.push({host:new URL(job.base).host,ok:false,error:error.name,ms:Date.now()-start});}
 if(results.length%16===0)console.log('Mirror checks completed: '+results.length+'/'+jobs.length);
 }}
 await Promise.all(Array.from({length:8},worker));
 const output=process.argv[2];if(!output||!path.isAbsolute(output))throw Error('Absolute output required');
 fs.mkdirSync(path.dirname(output),{recursive:true});fs.writeFileSync(output,JSON.stringify({builtin:builtin.length,dynamic:[...new Set(dynamic)].length,total:jobs.length,passed:results.filter(r=>r.ok).length,results},null,2));
 console.log(JSON.stringify({builtin:builtin.length,dynamic:[...new Set(dynamic)].length,total:jobs.length,passed:results.filter(r=>r.ok).length,report:output}));
})().catch(e=>{console.error(e.name);process.exitCode=1});
