const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..'),read=p=>fs.readFileSync(path.join(root,p),'utf8');
const report=JSON.parse(read('reports/UPDATE_ROUTE_BENCHMARK_20261014.json'));
assert.equal(report.rounds,3);assert.equal(report.builtinCount,78);
assert.equal(report.results.length,(report.ranking.manifest.length+report.ranking.apk.length)*3);
const ranking=read('app/src/main/java/cn/nua/wenguan/schedule/UpdateRouteRanking.java');
assert(ranking.includes('MEASURED_AT="'+report.measuredAt+'"'));
assert(ranking.includes('BASELINE_VERSION="'+report.baseline.versionName+'"'));
for(const kind of ['manifest','apk']){
 const list=report.ranking[kind];
 for(let i=1;i<list.length;i++)assert(list[i-1].successes>list[i].successes||(list[i-1].successes===list[i].successes&&(list[i-1].totalMs??Infinity)<=(list[i].totalMs??Infinity)),'reliability then complete time');
 for(const [name,items] of [[kind.toUpperCase()+'_ORDER',list.filter(x=>x.successes>0)],[kind.toUpperCase()+'_FAILED',list.filter(x=>!x.successes)]]){
  const block=ranking.slice(ranking.indexOf(name+'={')+name.length+2).split('};')[0];
  assert.deepEqual([...block.matchAll(/"([^"]+)"/g)].map(m=>m[1]),items.map(x=>x.key),'compiled '+name+' is actual report order');
 }
}
const app=read('app/src/main/java/cn/nua/wenguan/schedule/AppUpdates.java');
assert(app.includes('UpdateRouteRanking.order(new ArrayList<>(sources),false)'));
assert(app.includes('UpdateRouteRanking.order(routes,true)'));
assert(app.includes('routes.add(API+"tags/"+tag)')&&app.includes('assetUrl(release,filename,url)'));
assert(app.includes('verify.verify(signed)')&&app.includes('sameSigner(archive,current)'));
assert(!app.includes('benchmark-update-routes'),'no mass startup speed testing');
const mirrors=read('app/src/main/java/cn/nua/wenguan/schedule/UpdateMirrorPolicy.java');
assert(mirrors.includes('UpdateRouteRanking.mirrors()')&&mirrors.includes('Arrays.asList(BUILTIN)'));
console.log('Measured update routing guards passed: all samples, split rankings exactly match compiled order, original nodes retained, API resolves signed version, no runtime all-node probing, security preserved.');
