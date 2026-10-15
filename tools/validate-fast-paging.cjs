'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const root=path.resolve(__dirname,'..'),read=p=>fs.readFileSync(path.join(root,p),'utf8'),java=n=>read('app/src/main/java/cn/nua/wenguan/schedule/'+n+'.java');
const main=java('MainActivity'),host=java('ui/WeekPagerHost');
assert(main.includes('viewport.setPagingEnabled(page==1)')&&!main.includes('blockPagerTouch'),'no swallowing every new gesture while settling');
for(const token of ['captured=settling.getAsBoolean()','begin.run()','return horizontal','release.accept(direction,aborted)','finished.run()'])assert(host.includes(token),token);
assert(!host.includes('MotionEvent.obtain('),'stationary parent does not clone every input event');
assert(!host.includes('requestDisallowInterceptTouchEvent'),'native ViewGroup still honours the draft child interception veto');
const move=main.slice(main.indexOf('private void dragWeek'),main.indexOf('private void invalidatePages'));
for(const token of ['prepareNeighbor(','weekBody(','render(','scrollTo(','getSharedPreferences','measure('])assert(!move.includes(token),'MOVE hot path excludes '+token);
assert(move.includes('dragOrigin+(float)delta')&&move.includes('positionPages(offset)'));
const finish=main.slice(main.indexOf('@Override public void onAnimationEnd'),main.indexOf('private void resetWeekAnimation'));
assert(finish.includes('rotateWeek(settle)')&&!finish.includes('render()')&&!finish.includes('weekBody('),'settle does not rebuild home');
assert(main.includes('WeekPagerPolicy.rebased')&&main.includes('WeekPagerPolicy.nearest')&&main.includes('WeekPrefetchPolicy.trim'));
const warming=main.slice(main.indexOf('private final Runnable warmPages'),main.indexOf('@Override protected void onCreate'));
assert(warming.indexOf('warmWeek()')<warming.indexOf('buildSecondary(0)'),'weeks prepared before other tabs');
assert(warming.includes('body.measure')&&warming.includes('body.layout')&&warming.includes('body.getParent()==null'),'lookahead finishes text layout off the gesture path');
const grid=java('ui/WeekGrid'),background=java('ui/WeekGridBackground');
assert(grid.includes('new WeekGridBackground')&&!grid.includes('for(int period=1;period<=PeriodGrid.COUNT;period++)'));
const draw=background.slice(background.indexOf('@Override protected void onDraw'));
assert(!draw.includes('StaticLayout.Builder')&&!draw.includes('new TextView'),'cached axis text, not per-frame layout creation');
assert(grid.includes('CourseDetailsSheet.show')&&grid.includes('AgendaEditor.show')&&grid.includes('grid.reserveSpan'),'native click/edit and occupancy preserved');
assert(java('ui/GridOccupancyPolicy').includes('SchoolPeriodTimes.endTime(p)')&&java('ui/GridPlacementPolicy').includes('SchoolPeriodTimes.startTime(p)'));
const original='E:/南艺文管课表_备份/20261015_连续切周优化前/src/main/';
for(const file of ['java/cn/nua/wenguan/schedule/ScheduleSyncActivity.java','java/cn/nua/wenguan/schedule/widget/WidgetRecovery.java','java/cn/nua/wenguan/schedule/widget/TodayScheduleWidgetProvider.java','java/cn/nua/wenguan/schedule/widget/WeekScheduleWidgetProvider.java']){
    if(fs.existsSync(original+file)){const hash=p=>crypto.createHash('sha256').update(fs.readFileSync(p,'utf8').replace(/\r/g,'')).digest('hex');assert.equal(hash(path.join(root,'app/src/main',file)),hash(original+file),'unchanged stable feature: '+file);}
}
assert(read('app/build.gradle.kts').includes('versionCode = 33')&&read('app/build.gradle.kts').includes('versionName = "1.2.4"'));
console.log('Fast paging hot-path, continuous touch, bounded measured lookahead, lightweight grid and unchanged sync/widget guards passed. Device FPS pending.');
