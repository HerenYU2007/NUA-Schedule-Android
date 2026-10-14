// Bounded, anonymous benchmark of real signed manifests and complete APK downloads.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),dns=require('node:dns').promises;
const {performance}=require('node:perf_hooks');
const root=path.resolve(__dirname,'..'),out=process.argv[2];
if(!out||!path.isAbsolute(out))throw Error('Absolute output required');
const repo='HerenYU2007/NUA-Schedule-Android',github='https://github.com/'+repo+'/releases/',api='https://api.github.com/repos/'+repo+'/releases/';
const pub=fs.readFileSync(path.join(root,'app/src/main/assets/update-public.pem'));
const source=fs.readFileSync(path.join(root,'app/src/main/java/cn/nua/wenguan/schedule/UpdateMirrorPolicy.java'),'utf8');
const builtin=[...source.slice(source.indexOf('BUILTIN={'),source.indexOf('private UpdateMirrorPolicy')).matchAll(/"(https:\/\/[^"]+)"/g)].map(m=>new URL(m[1]).href);
function error(kind){const e=Error(kind);e.code=kind;return e;}
async function get(url,limit,accept='application/octet-stream'){
 const start=performance.now();let first=null,redirects=0;
 const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),20000);
 try{
  for(;;){
   const target=new URL(url);
   if(target.protocol!=='https:'||target.username||target.password||(target.port&&target.port!=='443'))throw error('unsafe-url');
   // Directory URLs are untrusted: never send benchmark requests to private IPs.
   const ips=await dns.lookup(target.hostname,{all:true});
   if(ips.some(({address:a})=>/^(127\.|10\.|192\.168\.|169\.254\.|0\.|172\.(1[6-9]|2\d|3[01])\.|::1$|f[cd]|fe80)/i.test(a)))throw error('private-host');
   const r=await fetch(url,{redirect:'manual',signal:controller.signal,headers:{Accept:accept,'User-Agent':'NUA-Schedule-Android','Accept-Encoding':'identity','Cache-Control':'no-cache','X-GitHub-Api-Version':'2022-11-28'}});
   if(first===null)first=performance.now()-start;
   if([301,302,303,307,308].includes(r.status)){
    const location=r.headers.get('location');await r.body?.cancel();
    if(!location||++redirects>=6)throw error('redirects');url=new URL(location,url).href;continue;
   }
   if(r.status!==200){await r.body?.cancel();throw error('http-'+r.status);}
   let size=0,blocks=[];for await(const block of r.body){size+=block.length;if(size>limit){controller.abort();throw error('oversize');}blocks.push(block);}
   return {bytes:Buffer.concat(blocks),headersMs:Math.round(first),totalMs:Math.round(performance.now()-start),size,redirects};
  }
 }catch(e){e.safeKind=e.code&&typeof e.code==='string'?e.code:e.name;e.elapsedMs=Math.round(performance.now()-start);throw e;}
 finally{clearTimeout(timer);}
}
function verify(bytes){
 const e=JSON.parse(bytes),body=Buffer.from(e.payload,'base64');
 if(e.algorithm!=='SHA256withRSA'||!crypto.verify('sha256',body,pub,Buffer.from(e.signature,'base64')))throw error('signature');
 const p=JSON.parse(body);if(p.packageName!=='cn.nua.wenguan.schedule'||p.versionCode!==expected.versionCode||p.apk.sha256!==expected.apk.sha256)throw error('stale-or-metadata');return p;
}
let expected;
(async()=>{
 const latest=JSON.parse((await get(api+'latest',1048576,'application/vnd.github+json')).bytes);
 const manifestAsset=latest.assets.find(a=>a.name==='update.json'),apkAsset=latest.assets.find(a=>a.name.endsWith('.apk'));
 if(!manifestAsset||!apkAsset)throw error('missing-asset');
 const envelope=(await get(manifestAsset.url,131072)).bytes;
 const e=JSON.parse(envelope),body=Buffer.from(e.payload,'base64');
 if(!crypto.verify('sha256',body,pub,Buffer.from(e.signature,'base64')))throw error('signature');
 expected=JSON.parse(body);
 let dynamic=[],directory=[];
 const extract=text=>[...text.replaceAll('\\/','/').replaceAll('\\"','"').matchAll(/["']?label["']?\s*:\s*["'](?:search|contribute)["']\s*,\s*["']?value["']?\s*:\s*["']([^"'\s]+)["']/g)].map(m=>new URL(m[1].startsWith('https://')?m[1]:'https://'+m[1]+'/').href).filter(u=>new URL(u).pathname==='/'&&!new URL(u).username&&!new URL(u).port);
 try{
  const p=await get('https://github.akams.cn/',1048576);directory.push({kind:'page',ok:true,totalMs:p.totalMs,headersMs:p.headersMs});const text=p.bytes.toString();dynamic.push(...extract(text));
  const chunks=[...new Set([...text.matchAll(/\/_next\/static\/chunks\/[A-Za-z0-9_.-]+\.js/g)].map(m=>m[0]))].slice(0,24);
  for(const p of chunks)try{const r=await get('https://github.akams.cn'+p,1048576);dynamic.push(...extract(r.bytes.toString()));directory.push({kind:p,ok:true,totalMs:r.totalMs,headersMs:r.headersMs});}catch(e){directory.push({kind:p,ok:false,error:e.safeKind,elapsedMs:e.elapsedMs});}
 }catch(e){directory.push({kind:'page',ok:false,error:e.safeKind,elapsedMs:e.elapsedMs});}
 const mirrors=[...new Set(['https://gh-proxy.org/',...dynamic,...builtin])];
 const jobs=mirrors.map(key=>({key,manifest:key+github+'latest/download/update.json',apk:key+expected.apk.url}));
 jobs.push({key:'direct',manifest:github+'latest/download/update.json',apk:expected.apk.url},{key:'api',manifest:manifestAsset.url,apk:apkAsset.url,metadata:{manifest:api+'latest',apk:api+'tags/'+latest.tag_name}},
  {key:'raw',manifest:'https://raw.githubusercontent.com/'+repo+'/master/updates/update.json'},
  {key:'github-raw',manifest:'https://github.com/'+repo+'/raw/refs/heads/master/updates/update.json'});
 const startedAt=new Date().toISOString(),results=[];
 for(let round=1;round<=3;round++){
  const tasks=jobs.flatMap(job=>['manifest',...(job.apk?['apk']:[])].map(kind=>({job,kind})));let index=0,done=0;
  // Rotate order each pass; modest concurrency limits transient load.
  tasks.push(...tasks.splice(0,(round-1)*37%tasks.length));
  async function worker(){for(;;){const task=tasks[index++];if(!task)return;const {job,kind}=task;try{
   const requestStarted=performance.now();let url=job[kind],metadataResult=null;
   if(job.metadata){
    metadataResult=await get(job.metadata[kind],1048576,'application/vnd.github+json');
    const release=JSON.parse(metadataResult.bytes),name=kind==='manifest'?'update.json':expected.apk.url.split('/').pop();
    const asset=release.assets.find(a=>a.name===name&&a.state==='uploaded');
    if(!asset||release.draft||release.prerelease||release.tag_name!==latest.tag_name)throw error('api-metadata');
    url=asset.url;
   }
   const r=await get(url,kind==='manifest'?131072:expected.apk.size+1);
   if(metadataResult){r.headersMs=metadataResult.headersMs;r.totalMs=Math.round(performance.now()-requestStarted);}
   if(kind==='manifest')verify(r.bytes);
   else if(r.size!==expected.apk.size||crypto.createHash('sha256').update(r.bytes).digest('hex')!==expected.apk.sha256)throw error('apk-hash-size');
   results.push({key:job.key,kind,round,ok:true,headersMs:r.headersMs,totalMs:r.totalMs,bytes:r.size});
  }catch(e){results.push({key:job.key,kind,round,ok:false,error:e.safeKind||e.code||e.name,elapsedMs:e.elapsedMs||null});}
  if(++done%30===0)console.log('Round '+round+': '+done+'/'+tasks.length+' checks completed');
  }}
  await Promise.all(Array.from({length:8},worker));
  fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,JSON.stringify({startedAt,measuredAt:new Date().toISOString(),baseline:{versionName:expected.versionName,versionCode:expected.versionCode,sha256:expected.apk.sha256,size:expected.apk.size},builtinCount:builtin.length,dynamicCount:new Set(dynamic).size,routeCount:jobs.length,rounds:round,directory,results},null,2));
  console.log('Round '+round+' complete, report checkpoint saved.');
 }
 const median=a=>{a=a.slice().sort((x,y)=>x-y);return a.length?Math.round((a[Math.floor((a.length-1)/2)]+a[Math.floor(a.length/2)])/2):null;};
 const ranking={};
 for(const kind of ['manifest','apk'])ranking[kind]=jobs.filter(j=>j[kind]).map(j=>{const r=results.filter(x=>x.key===j.key&&x.kind===kind),ok=r.filter(x=>x.ok);return {key:j.key,successes:ok.length,attempts:r.length,headersMs:median(ok.map(x=>x.headersMs)),totalMs:median(ok.map(x=>x.totalMs))};}).sort((a,b)=>b.successes-a.successes||(a.totalMs??Infinity)-(b.totalMs??Infinity)||a.key.localeCompare(b.key));
 const report=JSON.parse(fs.readFileSync(out));report.ranking=ranking;fs.writeFileSync(out,JSON.stringify(report,null,2));
 console.log(JSON.stringify({report:out,manifestTop:ranking.manifest.slice(0,8),apkTop:ranking.apk.slice(0,8),routes:jobs.length,checks:results.length}));
})().catch(e=>{console.error('Benchmark stopped: '+(e.safeKind||e.code||e.name));process.exitCode=1;});
