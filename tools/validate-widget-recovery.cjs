'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..'),read=p=>fs.readFileSync(path.join(root,p),'utf8'),java=n=>read('app/src/main/java/cn/nua/wenguan/schedule/'+n+'.java');
const manifest=read('app/src/main/AndroidManifest.xml');
assert.equal((manifest.match(/<meta-data\s+android:name="android.appwidget.provider"/g)||[]).length,4,'four distinct entries');
for(const name of ['TodayScheduleWidgetProvider','WhiteTodayScheduleWidgetProvider','WeekScheduleWidgetProvider','WhiteWeekScheduleWidgetProvider'])assert(manifest.includes('.widget.'+name));
const today=java('widget/TodayScheduleWidgetProvider');
const upgrade=today.slice(today.indexOf('} else if (Intent.ACTION_MY_PACKAGE_REPLACED'),today.indexOf('} else if (ACTION_REFRESH'));
assert(upgrade.includes('ensureInitialDay')&&!upgrade.includes('setDayOffset'),'upgrade must not force Tomorrow back to Today');
assert(today.includes('DailyWidgetSelectionPolicy.offset')&&today.includes('KEY_SELECTED_ON'));
assert(today.includes('new Intent(context, providerClass())')&&today.includes('new ComponentName(context, providerClass())'),'white daily actions target white daily');
assert(java('widget/WhiteTodayScheduleWidgetProvider').includes('white_widget_preferences'),'independent daily preference');
const week=java('widget/WeekScheduleWidgetProvider');
assert(week.includes('white?WhiteWeekScheduleWidgetProvider.class:WeekScheduleWidgetProvider.class')&&week.includes('refreshProvider(c,getClass())'));
assert(today.includes('OPTION_APPWIDGET_RESTORE_COMPLETED')&&week.includes('OPTION_APPWIDGET_RESTORE_COMPLETED'));
assert(week.includes('putLong("week_"+newIds[n]')&&week.includes('edit.remove("week_"+oldIds[n])'));
const changes=java('ScheduleChanges');assert(changes.indexOf('WidgetWork.refresh')<changes.indexOf('ReminderScheduler.scheduleNext'),'reminder failure cannot prevent widget refresh');
const recovery=java('widget/WidgetRecovery');
assert(recovery.includes('lastUpdateTime')&&recovery.includes('setMinimumLatency')&&recovery.includes('JOB+attempt'));
assert(recovery.includes('readRules(c)==null')&&recovery.includes('generation')&&recovery.includes('try{WhiteTodayScheduleWidgetProvider.refreshAll'));
assert(!recovery.includes('ScheduleSyncActivity')&&!recovery.includes('new WebView'));
assert(today.includes('ACTION_AUTO_TOMORROW.equals(action)||ACTION_RESET_TODAY.equals(action)')&&today.includes('WhiteTodayScheduleWidgetProvider.class).setAction(action)'),'white list receives automatic22/midnight switches too');
for(const token of ['CookieManager','loadUrl(','HttpURLConnection','saveLocal(','clearSchedule(','setImportedRules('])assert(!recovery.includes(token),'local repair only: '+token);
assert(manifest.includes('BIND_JOB_SERVICE')&&manifest.includes('USER_UNLOCKED'));
assert(java('widget/WidgetRecoveryReceiver').includes('goAsync()')&&java('widget/WidgetRecoveryReceiver').includes('pending::finish'));
const settings=java('SettingsActivity');for(const name of ['今日列表 · 深色','今日列表 · 白色','竖条周课表 · 深色','竖条周课表 · 白色'])assert(settings.includes(name));
for(const name of ['widget_phone','widget_today','widget_week']){
    const original=read('app/src/main/res/layout/'+name+'.xml'),white=read('app/src/main/res/layout/'+name+'_white.xml');
    const ids=s=>[...s.matchAll(/@\+id\/(\w+)/g)].map(m=>m[1]);assert.deepEqual(ids(original),ids(white),'white sizes/IDs mirror the existing layout');
    assert(white.includes('@drawable/bg_widget_white'));
}
for(const name of ['widget_picker_preview_white_bitmap','week_widget_preview_white']){
    const png=fs.readFileSync(path.join(root,'app/src/main/res/drawable-nodpi/'+name+'.png'));
    assert.equal(png.subarray(0,8).toString('hex'),'89504e470d0a1a0a');
}
console.log('Four widget theme/restore/recovery guards passed. No live launcher upgrade or device rendering claim.');
