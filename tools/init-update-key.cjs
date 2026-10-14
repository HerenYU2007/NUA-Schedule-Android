// Generated publishing key is intentionally outside the repository. Never print or upload it.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const target=process.argv[2]||'E:/南艺电子课表_发布密钥/release-private.pem';
if(!path.isAbsolute(target)||path.resolve(target).startsWith(path.resolve(__dirname,'..')+path.sep))throw new Error('Private key must be outside the source repository');
fs.mkdirSync(path.dirname(target),{recursive:true});
if(!fs.existsSync(target)){const keys=crypto.generateKeyPairSync('rsa',{modulusLength:3072,publicKeyEncoding:{type:'spki',format:'pem'},privateKeyEncoding:{type:'pkcs8',format:'pem'}});fs.writeFileSync(target,keys.privateKey,{mode:0o600,flag:'wx'});}
const publicKey=crypto.createPublicKey(fs.readFileSync(target)).export({type:'spki',format:'pem'});
fs.writeFileSync(path.join(__dirname,'../app/src/main/assets/update-public.pem'),publicKey);
console.log('Publishing key ready outside repository; only the public key was added to app assets.');
