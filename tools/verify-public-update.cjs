const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const root=path.resolve(__dirname,'..'),pub=fs.readFileSync(path.join(root,'app/src/main/assets/update-public.pem'));
const out=process.argv[2];if(!out||!path.isAbsolute(out))throw Error('Absolute output needed');
const original='https://github.com/HerenYU2007/NUA-Schedule-Android/releases/download/v1.2.0/';
async function fetchFile(url,dest,accept='application/octet-stream'){
 const r=await fetch(url,{signal:AbortSignal.timeout(30000),headers:{Accept:accept,'User-Agent':'NUA-Schedule-Android','Cache-Control':'no-cache','X-GitHub-Api-Version':'2022-11-28'}});
 if(!r.ok||new URL(r.url).protocol!=='https:')throw Error('HTTPS/HTTP');
 const bytes=Buffer.from(await r.arrayBuffer());if(bytes.length>1048576)throw Error('limit');
 fs.writeFileSync(dest,bytes);return bytes;
}
(async()=>{
 const cases=[
 ['direct',original+'update.json',original+'nua-schedule-1.2.0.apk'],
 ['preferred','https://gh-proxy.org/'+original+'update.json','https://gh-proxy.org/'+original+'nua-schedule-1.2.0.apk'],
 ['api','https://api.github.com/repos/HerenYU2007/NUA-Schedule-Android/releases/assets/640507707','https://api.github.com/repos/HerenYU2007/NUA-Schedule-Android/releases/assets/640507651'],
 ['raw','https://raw.githubusercontent.com/HerenYU2007/NUA-Schedule-Android/master/updates/update.json',null],
 ['github-raw','https://github.com/HerenYU2007/NUA-Schedule-Android/raw/refs/heads/master/updates/update.json',null]];
 let passed=0;
 await Promise.all(cases.map(async([name,manifest,apk])=>{
  const dir=path.join(out,name);fs.mkdirSync(dir,{recursive:true});
  try{
   const envelope=JSON.parse(await fetchFile(manifest,path.join(dir,'update.json'))),payload=Buffer.from(envelope.payload,'base64');
   if(!crypto.verify('sha256',payload,pub,Buffer.from(envelope.signature,'base64')))throw Error('signature');
   const data=JSON.parse(payload);if(data.versionCode!==29||data.versionName!=='1.2.0')throw Error('version');
   if(apk){const bytes=await fetchFile(apk,path.join(dir,'nua-schedule-1.2.0.apk'));if(bytes.length!==data.apk.size||crypto.createHash('sha256').update(bytes).digest('hex')!==data.apk.sha256)throw Error('APK');}
   passed++;console.log(name+': verified 1.2.0 '+(apk?'manifest/APK':'manifest'));
  }catch(e){console.log(name+': unavailable ('+e.name+')');}
 }));
 if(passed<3)process.exitCode=1;
 console.log('Verified public routes: '+passed+'/'+cases.length+'. No credentials or phone install used.');
})().catch(e=>{console.error(e.name);process.exitCode=1});
