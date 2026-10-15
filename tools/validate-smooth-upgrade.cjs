'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..'),read=p=>fs.readFileSync(path.join(root,p),'utf8'),java=n=>read('app/src/main/java/cn/nua/wenguan/schedule/'+n+'.java');
const main=java('MainActivity');
for(const token of ['Pane weekPane,beforePane,afterPane,agendaPane,minePane','WeekPagerPolicy.positions','positionPages((float)animation.getAnimatedValue())','weekPane=afterPane','weekPane=beforePane','attach(agendaPane.body','attach(minePane.body','if(!resumed||','dataChanged()','OutsideTapPolicy','cancelForOutsideTap','WeekPagerHost'])assert(main.includes(token),token);
assert(!main.includes('.withLayer()')&&!main.includes('translationX(-distance)'),'no outgoing-only animation or large forced layers');
const xml=read('app/src/main/res/layout/activity_main.xml');assert(xml.includes('page_viewport')&&xml.includes('android:clipChildren="true"'));
const grid=java('ui/WeekGrid');assert(grid.includes('AgendaGridPolicy.plan')&&grid.includes('if(lanes>1)')&&grid.includes('chooseItems(')&&grid.includes('重叠日程（'));
assert(grid.includes('grid.reserveSpan')&&grid.includes('grid.reserveTime'),'occupied padding and hidden overlap agendas cannot create draft courses');
assert(main.includes('renderAgendaConflicts(stored)')&&main.includes('时间冲突（'),'separate conflict section in agenda');
const draft=java('ui/InteractiveWeekGrid');assert(draft.includes('occupancy.free(day,first,first)')&&draft.includes('occupancy.free(day,span[0],span[1])'),'both blank hit and dragging enforce occupancy');assert(draft.includes('if(suppressBlankTap)return;')&&draft.includes('if(draft!=null){clearDraft();return;}'),'outside blank tap cancels rather than recreating');
assert(!draft.includes('saveLocal(')&&!draft.includes('AgendaStore.put('),'draft cancellation cannot change saved data');
const changes=java('ScheduleChanges');assert(changes.includes('private static void refreshNow')&&changes.includes('WidgetWork.refresh(c)')&&!changes.includes('sendBroadcast('),'direct off-main rebuild rather than bouncing work back onto main receivers');
for(const provider of ['TodayScheduleWidgetProvider','WeekScheduleWidgetProvider']){
    const source=java('widget/'+provider);assert(source.includes('goAsync()')&&source.includes('WidgetWork.enqueue')&&source.includes('pending::finish'));
    assert(source.includes('WidgetLayoutEpoch.layout')&&source.includes('WidgetHostPolicy.singleLayout'));
    assert(source.includes('WidgetDiagnostics.failure')&&source.includes('WidgetDiagnostics.submitted'));
}
const recovery=java('widget/WidgetRecovery');assert(recovery.includes('WidgetWork.enqueue')&&recovery.includes('WidgetDiagnostics.accepted()-successes>=widgetCount(c)'));
assert(!recovery.includes('.putLong("generation",generation).putLong("seen_update"'),'do not declare upgrade recovered before a submission');
const epoch=java('widget/WidgetLayoutEpoch');assert(epoch.includes('lastUpdateTime')&&epoch.includes('public static synchronized void force'));
const ids=new Map([...read('app/stable-resource-ids.txt').matchAll(/cn\.nua\.wenguan\.schedule:([^\s]+) = (0x[0-9a-f]+)/g)].map(m=>[m[1],m[2]]));
assert(ids.size>0&&new Set(ids.values()).size===ids.size,'stable resource map nonempty and collision-free');
for(const base of ['widget_phone','widget_today','widget_week','widget_phone_white','widget_today_white','widget_week_white']){
    assert(ids.get('layout/'+base+'_a')!==ids.get('layout/'+base+'_b'),'distinct re-inflation identities');
    for(const suffix of ['a','b'])assert(read('app/src/main/res/layout/'+base+'_'+suffix+'.xml').includes('layout="@layout/'+base+'"'));
}
assert(read('app/build.gradle.kts').includes('"--stable-ids"'),'freeze resource IDs across builds');
const symbolsPath=path.join(root,'app/build/intermediates/runtime_symbol_list/debug/processDebugResources/R.txt');
if(fs.existsSync(symbolsPath)){
    const symbols=new Map([...fs.readFileSync(symbolsPath,'utf8').replace(/\r/g,'').matchAll(/^int (\w+) (\w+) (0x[0-9a-f]+)$/gm)].map(m=>[m[1]+'/'+m[2],m[3]]));
    assert.equal(ids.size,symbols.size,'every current compiled resource has a stable mapping; append future new resource IDs without changing existing ones');
    for(const [name,id] of ids)assert.equal(symbols.get(name),id,'compiled stable ID: '+name);
}
const updates=java('AppUpdates');assert(!updates.includes('Stale manifest')&&updates.includes('UpdateCheckPolicy.hasUpdate')&&updates.includes('toast(observer,"暂无更新")'));
assert(updates.includes('manualObserver=manual?')&&updates.includes('if(checking)manualObserver='),'manual tap joins an in-flight automatic check, automatic no-update is silent');
for(const guard of ['verify.verify(signed)','release.getInt("schemaVersion")','apk.getString("sha256")','sameSigner(archive,current)','code(archive)<=code(current)','Public host only'])assert(updates.includes(guard),guard);
assert(updates.indexOf('verify.verify(signed)')<updates.indexOf('return release;'),'untrusted/invalid responses never count as no update');
assert(updates.includes('更新检查失败，请稍后重试'),'network errors remain distinct');
console.log('Smooth paging / outside cancellation / off-main widget recovery / stable IDs / update result guards passed. No device FPS or launcher upgrade claim.');
