const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..'),read=p=>fs.readFileSync(path.join(root,p),'utf8');
const main=read('app/src/main/java/cn/nua/wenguan/schedule/MainActivity.java');
const start=main.slice(main.indexOf('protected void onStart'),main.indexOf('private void selectPage'));
assert(start.includes('AppUpdates.checkOnOpen(this)'));
const updates=read('app/src/main/java/cn/nua/wenguan/schedule/AppUpdates.java');
assert(updates.includes('WORK.execute')&&updates.includes('newSingleThreadExecutor'));
assert(!updates.includes('JobScheduler')&&!updates.includes('AlarmManager'));
assert(updates.includes('canPresentSoftwareUpdate()'),'automatic result respects empty import dialog');
const prompt=main.slice(main.indexOf('private boolean showEmptySchedulePrompt'),main.indexOf('private void render()'));
assert(prompt.includes('emptyPromptHandled||ScheduleRepository.hasImportedRules()'));
assert(prompt.indexOf('emptyPromptHandled=true')<prompt.indexOf('new AlertDialog.Builder'),'deduplicate before focus loss');
assert(main.includes('state.putBoolean("empty_prompt_handled",emptyPromptHandled)')&&main.includes('state.getBoolean("empty_prompt_handled",false)'),'recreation retains dismissal');
assert(prompt.includes('.setPositiveButton("登录获取",(d,w)->openScheduleImport(false))'));
assert(prompt.includes('.setNeutralButton("导入 XLS",(d,w)->openScheduleImport(true))'));
assert(prompt.includes('.setNegativeButton("稍后",null)')&&prompt.includes('intent.putExtra("manual_import",true)'));
assert(!prompt.includes('clearSchedule(')&&!prompt.includes('save('),'empty state cannot replace data automatically');
const mine=main.slice(main.indexOf('private void renderMine'),main.indexOf('private View courseCard'));
for(const token of ['R.drawable.ic_ui_cloud','R.drawable.ic_ui_file','R.drawable.ic_ui_settings','R.drawable.ic_ui_update','R.color.action_green','R.color.action_purple','R.color.action_amber','view_mine_summary'])assert(mine.includes(token),'distinct roles: '+token);
assert(!mine.includes('new Button'));
const card=read('app/src/main/java/cn/nua/wenguan/schedule/ui/MineActionCards.java');
assert(card.includes('RippleDrawable')&&card.includes('AccessibilityNodeInfo')&&card.includes('setContentDescription'));
assert(!read('app/src/main/res/layout/view_mine_action.xml').includes('android:maxLines'),'wrap large text instead of clipping descriptions');
for(const dir of ['values','values-night'])assert(read('app/src/main/res/'+dir+'/styles.xml').includes('<item name="android:buttonStyle">@style/ModernButton</item>'));
const styles=read('app/src/main/res/values/styles.xml');
for(const value of ['@drawable/bg_button_secondary','@drawable/bg_button_primary','@drawable/bg_button_danger','@drawable/bg_nav_tab','<item name="android:textAllCaps">false</item>'])assert(styles.includes(value));
const settings=read('app/src/main/res/layout/activity_settings.xml');
assert.equal((settings.match(/style="@style\/SettingsAction(?:Danger)?"/g)||[]).length,7);
function colors(dir){return Object.fromEntries([...read('app/src/main/res/'+dir+'/colors.xml').matchAll(/name="([^"]+)">#([0-9A-Fa-f]{6})</g)].map(m=>[m[1],m[2]]));}
function lum(hex){return [0,2,4].map(i=>parseInt(hex.slice(i,i+2),16)/255).map(n=>n<=.04045?n/12.92:((n+.055)/1.055)**2.4).reduce((a,n,i)=>a+n*[.2126,.7152,.0722][i],0);}
function contrast(a,b){let x=lum(a),y=lum(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
for(const dir of ['values','values-night']){const c=colors(dir);assert(contrast(c.button_primary,c.on_button_primary)>=4.5,'primary contrast '+dir);for(const role of ['action_green','action_purple','action_amber','action_danger'])assert(contrast(c[role],c.surface)>=4.5,role+' contrast '+dir);}
console.log('1.2.1 UI guards passed: empty prompt dedup/recreation/import routes, no auto login, foreground async checks, distinct action cards, global light/dark rounded buttons and text contrast. Device lifecycle/visual verification still required.');
