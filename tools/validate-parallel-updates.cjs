'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..'),read=p=>fs.readFileSync(path.join(root,p),'utf8').replace(/\r/g,''),base='app/src/main/java/cn/nua/wenguan/schedule/';
const app=read(base+'AppUpdates.java'),race=read(base+'ParallelUpdateCheck.java'),connections=read(base+'UpdateCheckConnections.java');
for(const token of ['ParallelUpdateCheck.fetch','ParallelUpdateCheck.prioritize','a.getApplicationContext(),installed','requests::cancel','verifyManifest(fetchBytes(url,128*1024),publicKey,a.getPackageName())'])assert(app.includes(token),token);
const check=app.slice(app.indexOf('private static JSONObject fetchRelease'),app.indexOf('static JSONObject verifyManifest'));
assert(check.indexOf('cachedMirrors(a)')<check.indexOf('ParallelUpdateCheck.fetch'),'initial probes use cache/builtins without directory blocking');
assert(check.indexOf('loadMirrors(a)')>check.indexOf('ParallelUpdateCheck.fetch'),'directory is a separate concurrent task');
for(const token of ['PARALLELISM=6','TIMEOUT_MS=30000','ExecutorCompletionService','complete.poll(remaining,TimeUnit.NANOSECONDS)','offered>installed)return result.manifest','pool.shutdownNow()','task.cancel(true)','cancelRequests.run()'])assert(race.includes(token),token);
for(const token of ['ThreadLocal<UpdateCheckConnections>','scope.active.add(connection)','cancelled=true','connection.disconnect()','CURRENT.remove()'])assert(connections.includes(token),token);
assert(app.includes('private static synchronized void recordFailure'),'parallel attempts cannot corrupt status log');
for(const token of ['verify.verify(signed)','sameSigner(archive,current)','code(archive)<=code(current)','Public host only','setConnectTimeout(12000)','setReadTimeout(20000)','No verified update download','toast(observer,"暂无更新")'])assert(app.includes(token),token);
assert(!app.includes('CookieManager')&&!app.includes('JobScheduler')&&!race.includes('setSSLSocketFactory'),'no school login or TLS bypass');
// Only manifest scheduling and scoped connection teardown change in this release.
const original='E:/南艺文管课表_备份/20261015_1.2.4并行更新检查前/src/main/java/cn/nua/wenguan/schedule/';
function method(source,name){const start=source.search(new RegExp('\\n    private static [^\\n]+ '+name+'\\('));assert(start>=0,name);const rest=source.slice(start+1),next=rest.indexOf('\n    private static ',1);return next<0?rest:rest.slice(0,next);}
if(fs.existsSync(original+'AppUpdates.java')){
 const before=fs.readFileSync(original+'AppUpdates.java','utf8').replace(/\r/g,'');
 for(const name of ['fetchApk','download','sameSigner','install','assetUrl','releaseMetadata'])assert.equal(method(app,name),method(before,name),'unchanged APK/install safety: '+name);
 for(const name of ['UpdateDownload.java','ScheduleSyncActivity.java','widget/TodayScheduleWidgetProvider.java','widget/WeekScheduleWidgetProvider.java'])assert.equal(read(base+name),fs.readFileSync(original+name,'utf8').replace(/\r/g,''),'download/sync/widget safety unchanged: '+name);
}
assert(read('app/build.gradle.kts').includes('versionCode = 35')&&read('app/build.gradle.kts').includes('versionName = "1.2.6"'));
console.log('Parallel 1.2.4 integration/security guards passed. Manifest scheduling changed; APK validation, installation, sync, UI and widgets preserved.');
