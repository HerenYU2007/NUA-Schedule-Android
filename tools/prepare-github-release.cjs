const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
// Usage: node tools/prepare-github-release.cjs owner/repository /absolute/output /absolute/private-key
const [repo,out,key,notesPath]=process.argv.slice(2);
if(!/^[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+$/.test(repo||'')||!path.isAbsolute(out||'')||!path.isAbsolute(key||''))throw new Error('Supply owner/repository and absolute output/private-key paths');
const root=path.resolve(__dirname,'..'),build=path.join(root,'app/build/outputs/apk/debug');
const meta=JSON.parse(fs.readFileSync(path.join(build,'output-metadata.json'),'utf8')),element=meta.elements[0];
if(meta.applicationId!=='cn.nua.wenguan.schedule'||!/^\d+\.\d+\.\d+$/.test(element.versionName)||!Number.isSafeInteger(element.versionCode))throw new Error('Invalid build metadata');
const bytes=fs.readFileSync(path.join(build,element.outputFile)),hash=crypto.createHash('sha256').update(bytes).digest('hex');
const file=`nua-schedule-${element.versionName}.apk`;
const notes=notesPath?fs.readFileSync(notesPath,'utf8'):'请在发布前提供本版更新说明文件。';
if(notes.length>16000)throw new Error('Release notes too long');
const manifest={schemaVersion:1,packageName:meta.applicationId,versionCode:element.versionCode,versionName:element.versionName,notes,apk:{url:`https://github.com/${repo}/releases/download/v${element.versionName}/${file}`,size:bytes.length,sha256:hash}};
const payload=Buffer.from(JSON.stringify(manifest)),signature=crypto.sign('sha256',payload,fs.readFileSync(key));
const publicKey=fs.readFileSync(path.join(root,'app/src/main/assets/update-public.pem'));
if(!crypto.verify('sha256',payload,publicKey,signature))throw new Error('Private key does not match app public key');
fs.mkdirSync(out,{recursive:true});fs.writeFileSync(path.join(out,file),bytes);
fs.writeFileSync(path.join(out,'update.json'),JSON.stringify({algorithm:'SHA256withRSA',payload:payload.toString('base64'),signature:signature.toString('base64')},null,2));
fs.writeFileSync(path.join(out,'SHA256SUMS.txt'),`${hash}  ${file}\n`);
console.log('Release assets prepared locally. Nothing uploaded. Do not edit signed update.json by hand.');
