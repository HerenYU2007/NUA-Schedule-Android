package cn.nua.wenguan.schedule;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import cn.nua.wenguan.schedule.data.*;
import cn.nua.wenguan.schedule.model.*;
import cn.nua.wenguan.schedule.ui.*;
import cn.nua.wenguan.schedule.widget.TodayScheduleWidgetProvider;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppearanceActivity {
    private LocalDate selectedDate, renderedToday;
    private int page=1; private boolean expandedCompleted=true;
    private LinearLayout content; private WeekSwipeScrollView scroll;
    private boolean emptyPromptHandled;
    private AlertDialog emptyPrompt;
    private boolean openingImport;
    private boolean firstResume=true,resumed;
    private TextView agendaCountdown;
    private List<CourseSession> agendaClasses=java.util.Collections.emptyList();
    private String agendaStatus="";
    private boolean cachedGrid;
    private LinearLayout agendaPage,minePage;
    private int navigationGeneration,animationGeneration;
    private WeekPagerHost viewport;
    private Pane weekPane,beforePane,afterPane,agendaPane,minePane;
    private android.animation.ValueAnimator pagerAnimation;
    private String knownRules,knownAgenda,knownClock;private LocalDate knownStart;private boolean knownDark;
    private final OutsideTapPolicy outsideTap=new OutsideTapPolicy();
    private InteractiveWeekGrid touchedDraft;
    private float dragOrigin;
    private int prefetchDirection=1;
    private static final class Pane {
        final WeekSwipeScrollView scroll;final LinearLayout body;LocalDate date;
        Pane(WeekSwipeScrollView scroll,LinearLayout body){this.scroll=scroll;this.body=body;}
    }
    private final java.util.LinkedHashMap<LocalDate,LinearLayout> weekViews=new java.util.LinkedHashMap<>(8,.75f,true);
    private final Handler ticker=new Handler(Looper.getMainLooper());
    private final Runnable tick=new Runnable(){@Override public void run(){if(!isFinishing()){
        // Never tear down a scrolling hierarchy for a countdown tick.
        if(!scroll.isInteracting()&&!switchingWeek){
            if(!ScheduleRepository.today().equals(renderedToday)){invalidatePages();render();}
            else if(page==0)updateAgendaStatus();
        }
        ticker.postDelayed(this,30000);
    }}};
    private void requestWarmPages(long delay){ticker.removeCallbacks(warmPages);if(resumed)ticker.postDelayed(warmPages,delay);}
    private final Runnable warmPages=new Runnable(){@Override public void run(){
        if(!resumed||isFinishing()||isDestroyed())return;
        if(switchingWeek||viewport.isTouching()||((page!=1||viewport.hasVerticalTail())&&scroll.isInteracting())){requestWarmPages(48);return;}
        // Adjacent weeks first, then farther lookahead. Other tabs never starve paging prefetch.
        if(page==1&&warmWeek()) {requestWarmPages(32);return;}
        if(agendaPage==null)attach(agendaPane.body,buildSecondary(0));
        else if(minePage==null)attach(minePane.body,buildSecondary(2));
        else return;
        requestWarmPages(32);
    }};
    private boolean warmWeek(){
        for(LocalDate target:WeekPrefetchPolicy.lookahead(selectedDate,prefetchDirection)){
            long distance=java.time.temporal.ChronoUnit.WEEKS.between(selectedDate,target);
            if(Math.abs(distance)==1){
                Pane pane=distance<0?beforePane:afterPane;
                if(!target.equals(pane.date)){prepareNeighbor(distance<0?-1:1);return true;}
            }else if(!weekViews.containsKey(target)){
                LinearLayout body=weekBody(target);
                if(body.getParent()==null){int width=Math.max(1,(int)pagerWidth()-dp(40));body.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));body.layout(0,0,width,body.getMeasuredHeight());}
                return true;
            }
        }
        return false;
    }
    private boolean validWeekDate(LocalDate date){return date.getYear()>=1900&&date.getYear()<=2200;}
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); LegacySyncCleanup.cancel(this); setContentView(R.layout.activity_main);
        ScheduleRuleStore.applySaved(this);
        selectedDate=state!=null&&state.containsKey("selected_date")?safeDate(state.getLong("selected_date")):safeDate(getIntent().getLongExtra("selected_date",ScheduleRepository.today().toEpochDay()));
        if(getIntent().getBooleanExtra("show_grid",false))AppearanceSettings.saveGrid(this,true);
        page=state==null?(getIntent().getBooleanExtra("open_agenda",false)?0:1):state.getInt("page",1);
        emptyPromptHandled=state!=null&&state.getBoolean("empty_prompt_handled",false);
        content=findViewById(R.id.page_content);scroll=findViewById(R.id.page_scroll);setupPager();
        findViewById(R.id.tab_agenda).setOnClickListener(v->selectPage(0));
        findViewById(R.id.tab_timetable).setOnClickListener(v->selectPage(1));
        findViewById(R.id.tab_mine).setOnClickListener(v->selectPage(2));
        findViewById(R.id.add_course).setOnClickListener(v->startActivity(new Intent(this,CourseEditorActivity.class)));
        findViewById(R.id.select_date).setOnClickListener(v->datePicker());
        findViewById(R.id.page_date).setOnClickListener(v->{if(page==1)datePicker();});
        findViewById(R.id.display_mode).setOnClickListener(v->{if(AppearanceSettings.saveGrid(this,!AppearanceSettings.grid(this))){clearDraft();resetWeekAnimation();render();}else toast("显示模式保存失败");});
        render();
    }
    // Cold launch and reopening the main screen: network work remains off the UI thread.
    @Override protected void onStart(){super.onStart();cn.nua.wenguan.schedule.widget.WidgetRecovery.ensureAfterUpgrade(this);AppUpdates.checkOnOpen(this);}
    private Pane newPane(){
        WeekSwipeScrollView view=new WeekSwipeScrollView(this,null);view.setFillViewport(true);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(20),dp(20),dp(20));
        view.addView(body,new android.widget.FrameLayout.LayoutParams(-1,-2));
        view.setVisibility(View.INVISIBLE);viewport.addView(view,new android.widget.FrameLayout.LayoutParams(-1,-1));return new Pane(view,body);
    }
    private void setupPager(){
        viewport=findViewById(R.id.page_viewport);weekPane=new Pane(scroll,content);
        beforePane=newPane();afterPane=newPane();agendaPane=newPane();minePane=newPane();
        viewport.setCallbacks(this::beginWeekGesture,this::dragWeek,this::releaseWeek,()->switchingWeek);
        viewport.setOnGestureFinished(()->requestWarmPages(16));
        viewport.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol){resetWeekAnimation();positionPages(0);}});
    }
    private void attach(LinearLayout parent,View body){
        if(body.getParent()==parent&&parent.getChildCount()==1)return;
        if(body.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)body.getParent()).removeView(body);
        parent.removeAllViews();parent.addView(body);
    }
    private void clearDraft(){if(content!=null){InteractiveWeekGrid grid=InteractiveWeekGrid.findDraft(content);if(grid!=null)grid.clearDraft();}}
    @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){
        int action=event.getActionMasked();
        if(action==android.view.MotionEvent.ACTION_DOWN){
            touchedDraft=page==1&&content!=null?InteractiveWeekGrid.findDraft(content):null;
            outsideTap.down(event.getRawX(),event.getRawY(),android.view.ViewConfiguration.get(this).getScaledTouchSlop(),touchedDraft!=null&&!touchedDraft.draftContains(event.getRawX(),event.getRawY()));
        }
        if(action==android.view.MotionEvent.ACTION_MOVE)outsideTap.move(event.getRawX(),event.getRawY());
        if(event.getPointerCount()>1||action==android.view.MotionEvent.ACTION_CANCEL)outsideTap.cancel();
        InteractiveWeekGrid cleared=null;
        if(action==android.view.MotionEvent.ACTION_UP&&outsideTap.up(event.getRawX(),event.getRawY())&&touchedDraft!=null&&touchedDraft.hasDraft()){cleared=touchedDraft;cleared.cancelForOutsideTap();}
        try{return super.dispatchTouchEvent(event);}
        finally{if(cleared!=null)cleared.finishOutsideTap();if(action==android.view.MotionEvent.ACTION_UP||action==android.view.MotionEvent.ACTION_CANCEL)touchedDraft=null;}
    }
    private void selectPage(int selected){
        if(page==selected)return;clearDraft();resetWeekAnimation();scroll.stopInertia();page=selected;
        int[] tabs={R.id.tab_agenda,R.id.tab_timetable,R.id.tab_mine};for(int i=0;i<tabs.length;i++)findViewById(tabs[i]).setSelected(i==page);
        int token=++navigationGeneration;
        viewport.postOnAnimation(()->{if(token==navigationGeneration&&!isFinishing()&&!isDestroyed())render();});
    }
    private boolean switchingWeek;
    private float pagerWidth(){return Math.max(1,viewport.getWidth()>0?viewport.getWidth():getResources().getDisplayMetrics().widthPixels);}
    private void positionPages(float offset){
        if(weekPane==null)return;float[] positions=WeekPagerPolicy.positions(pagerWidth(),offset);
        beforePane.scroll.setTranslationX(positions[0]);weekPane.scroll.setTranslationX(positions[1]);afterPane.scroll.setTranslationX(positions[2]);
    }
    private void prepareNeighbor(int direction){
        Pane pane=direction<0?beforePane:afterPane;LocalDate target=selectedDate.plusWeeks(direction);
        if(target.equals(pane.date)){pane.scroll.scrollTo(0,weekPane.scroll.getScrollY());return;}
        if(target.getYear()<1900||target.getYear()>2200)return;
        attach(pane.body,weekBody(target));pane.date=target;int y=weekPane.scroll.getScrollY();pane.scroll.scrollTo(0,y);pane.scroll.post(()->{if(target.equals(pane.date))pane.scroll.scrollTo(0,y);});
    }
    private void updateWeekHeader(){
        LocalDate date=selectedDate;int week=ScheduleRepository.weekNumber(date);
        String title=date.toString().replace('-','/')+" 周"+"一二三四五六日".charAt(date.getDayOfWeek().getValue()-1);
        String subtitle=(week>0?"第"+week+"周 · ":"")+(cachedGrid?"竖条课表":"列表课表");
        TextView day=findViewById(R.id.page_date),detail=findViewById(R.id.page_subtitle);
        if(!title.contentEquals(day.getText()))day.setText(title);
        if(!subtitle.contentEquals(detail.getText()))detail.setText(subtitle);
    }
    private void rotateWeek(int direction){
        if(direction==0)return;
        selectedDate=selectedDate.plusWeeks(direction);Pane old=weekPane;
        if(direction>0){weekPane=afterPane;afterPane=beforePane;beforePane=old;}
        else{weekPane=beforePane;beforePane=afterPane;afterPane=old;}
        old.date=selectedDate.minusWeeks(direction);
        scroll=weekPane.scroll;content=weekPane.body;weekPane.scroll.bringToFront();prefetchDirection=direction;
        weekViews.get(selectedDate);updateWeekHeader();
    }
    private void beginWeekGesture(){
        ticker.removeCallbacks(warmPages);
        float offset=weekPane.scroll.getTranslationX();
        // Freeze at the visible position, then rebase to the nearest week without a visual jump.
        if(pagerAnimation!=null){animationGeneration++;pagerAnimation.cancel();pagerAnimation=null;switchingWeek=false;
            int nearest=WeekPagerPolicy.nearest(offset,pagerWidth());if(nearest!=0){clearDraft();rotateWeek(nearest);offset=WeekPagerPolicy.rebased(offset,pagerWidth(),nearest);}
            positionPages(offset);
        }
        dragOrigin=offset;
        // Creation is outside the MOVE loop; normally these are already attached/premeasured.
        prepareNeighbor(-1);prepareNeighbor(1);
    }
    private void releaseWeek(int gesture,boolean cancelled){
        changeWeek(WeekPagerPolicy.release(weekPane.scroll.getTranslationX(),pagerWidth(),gesture,cancelled));
    }
    private void changeWeek(int direction){
        if(switchingWeek||page!=1)return;
        if(direction!=0){LocalDate target=selectedDate.plusWeeks(direction);if(!validWeekDate(target))direction=0;else prefetchDirection=direction;}
        switchingWeek=true;if(direction!=0)clearDraft();final int settle=direction;int token=++animationGeneration;
        float from=weekPane.scroll.getTranslationX(),to=-direction*pagerWidth();
        pagerAnimation=android.animation.ValueAnimator.ofFloat(from,to);
        pagerAnimation.setDuration(WeekPagerPolicy.duration(pagerWidth(),from,to));pagerAnimation.setInterpolator(new android.view.animation.DecelerateInterpolator(2f));
        pagerAnimation.addUpdateListener(animation->positionPages((float)animation.getAnimatedValue()));
        pagerAnimation.addListener(new android.animation.AnimatorListenerAdapter(){
            @Override public void onAnimationEnd(android.animation.Animator animation){
                if(token!=animationGeneration)return;
                rotateWeek(settle);switchingWeek=false;pagerAnimation=null;dragOrigin=0;positionPages(0);
                // The incoming hierarchy is already attached. Do not run the full screen renderer.
                requestWarmPages(16);
            }
        });pagerAnimation.start();
    }
    private void resetWeekAnimation(){animationGeneration++;if(pagerAnimation!=null){pagerAnimation.cancel();pagerAnimation=null;}if(weekPane!=null)positionPages(0);dragOrigin=0;switchingWeek=false;}
    private void dragWeek(double delta){
        if(switchingWeek||page!=1)return;
        float offset=dragOrigin+(float)delta;LocalDate target=selectedDate.plusWeeks(offset<0?1:-1);
        if(!validWeekDate(target)){positionPages(0);return;}
        positionPages(offset); // No parsing, page creation, scrollTo, or requestLayout in MOVE.
    }
    private void invalidatePages(){
        clearDraft();weekViews.clear();agendaPage=null;minePage=null;
        if(beforePane!=null){beforePane.date=null;afterPane.date=null;weekPane.date=null;}
    }
    private boolean dataChanged(){
        if(!java.util.Objects.equals(knownClock,SchoolPeriodTimes.revision()))return true;
        return !java.util.Objects.equals(knownRules,getSharedPreferences("official_schedule",0).getString("rules",""))
                ||!java.util.Objects.equals(knownAgenda,AgendaStore.revision(this))
                ||!java.util.Objects.equals(knownStart,ScheduleRepository.schoolStartDate())
                ||knownDark!=AppearanceSettings.dark(this)
                ||!ScheduleRepository.today().equals(renderedToday);
    }
    private void rememberData(){
        knownRules=getSharedPreferences("official_schedule",0).getString("rules","");knownAgenda=AgendaStore.revision(this);
        knownClock=SchoolPeriodTimes.revision();knownStart=ScheduleRepository.schoolStartDate();knownDark=AppearanceSettings.dark(this);
    }
    private LocalDate safeDate(long epoch){try{LocalDate value=LocalDate.ofEpochDay(epoch);return value.getYear()>=1900&&value.getYear()<=2200?value:ScheduleRepository.today();}catch(RuntimeException invalid){return ScheduleRepository.today();}}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);clearDraft();resetWeekAnimation();if(intent.hasExtra("selected_date")){selectedDate=safeDate(intent.getLongExtra("selected_date",ScheduleRepository.today().toEpochDay()));page=1;if(intent.getBooleanExtra("show_grid",false))AppearanceSettings.saveGrid(this,true);}else if(intent.getBooleanExtra("open_agenda",false))page=0;render();}
    @Override protected void onResume(){super.onResume();resumed=true;openingImport=false;if(!firstResume){ScheduleRuleStore.applySaved(this);if(dataChanged())invalidatePages();if(content!=null)render();}firstResume=false;refreshWidget();AppUpdates.resumeInstall(this);ticker.removeCallbacks(tick);ticker.postDelayed(tick,30000);requestWarmPages(16);}
    @Override protected void onPause(){resumed=false;viewport.cancelGesture();ticker.removeCallbacks(tick);ticker.removeCallbacks(warmPages);navigationGeneration++;clearDraft();resetWeekAnimation();super.onPause();}
    @Override public void onWindowFocusChanged(boolean focused){
        super.onWindowFocusChanged(focused);
        if(focused&&content!=null&&!isFinishing()&&!isDestroyed()){
            if(showEmptySchedulePrompt())return;
            if(canPresentSoftwareUpdate())AppUpdates.presentPending(this);
        }
    }
    @Override protected void onSaveInstanceState(Bundle state){state.putLong("selected_date",selectedDate.toEpochDay());state.putInt("page",page);state.putBoolean("empty_prompt_handled",emptyPromptHandled);super.onSaveInstanceState(state);}
    @Override protected void onDestroy(){if(emptyPrompt!=null){emptyPrompt.setOnDismissListener(null);emptyPrompt.dismiss();emptyPrompt=null;}super.onDestroy();}
    private boolean showEmptySchedulePrompt(){
        if(emptyPromptHandled||ScheduleRepository.hasImportedRules())return false;
        // Mark before showing, so focus changes, rotation and returning from import cannot loop.
        emptyPromptHandled=true;
        emptyPrompt=new AlertDialog.Builder(this).setIcon(R.drawable.ic_ui_calendar)
                .setTitle("先导入你的课表").setMessage("当前还没有可显示的课表。\n可登录学校教务系统自动获取，或导入学校导出的 XLS 文件。导入前会显示预览供你确认。")
                .setPositiveButton("登录获取",(d,w)->openScheduleImport(false))
                .setNeutralButton("导入 XLS",(d,w)->openScheduleImport(true))
                .setNegativeButton("稍后",null).create();
        emptyPrompt.setOnDismissListener(d->{emptyPrompt=null;content.post(()->{
            if(!isFinishing()&&!isDestroyed()&&hasWindowFocus()&&canPresentSoftwareUpdate())AppUpdates.presentPending(this);
        });});
        emptyPrompt.show();return true;
    }
    private void openScheduleImport(boolean manual){
        openingImport=true;
        Intent intent=new Intent(this,ScheduleSyncActivity.class);
        if(manual)intent.putExtra("manual_import",true);
        startActivity(intent);
    }
    boolean canPresentSoftwareUpdate(){return emptyPrompt==null&&!openingImport;}
    private void render(){
        if(knownRules!=null&&dataChanged())invalidatePages();
        boolean grid=AppearanceSettings.grid(this);if(cachedGrid!=grid){clearDraft();weekViews.clear();beforePane.date=null;afterPane.date=null;weekPane.date=null;cachedGrid=grid;}
        if(!ScheduleRepository.today().equals(renderedToday))invalidatePages();
        Pane active=page==1?weekPane:page==0?agendaPane:minePane;scroll=active.scroll;content=active.body;
        weekPane.scroll.setVisibility(page==1?View.VISIBLE:View.INVISIBLE);beforePane.scroll.setVisibility(page==1?View.VISIBLE:View.INVISIBLE);afterPane.scroll.setVisibility(page==1?View.VISIBLE:View.INVISIBLE);
        agendaPane.scroll.setVisibility(page==0?View.VISIBLE:View.INVISIBLE);minePane.scroll.setVisibility(page==2?View.VISIBLE:View.INVISIBLE);
        active.scroll.bringToFront();positionPages(0);
        renderedToday=ScheduleRepository.today();
        LocalDate headerDate=page==0?ScheduleRepository.today():selectedDate;
        ((TextView)findViewById(R.id.page_date)).setText(page==2?"我的":headerDate.toString().replace('-','/')+" 周"+"一二三四五六日".charAt(headerDate.getDayOfWeek().getValue()-1));
        int week=ScheduleRepository.weekNumber(selectedDate);
        ((TextView)findViewById(R.id.page_subtitle)).setText(page==1?(week>0?"第"+week+"周 · ":"")+(AppearanceSettings.grid(this)?"竖条课表":"列表课表"):page==0?"当天课程与个人事项":"南艺电子课表 · 数据保存在本机");
        findViewById(R.id.week_controls).setVisibility(page==1?View.VISIBLE:View.GONE);
        findViewById(R.id.display_mode).setVisibility(page==1?View.VISIBLE:View.GONE);
        ((TextView)findViewById(R.id.display_mode)).setText(AppearanceSettings.grid(this)?"竖条课表 · 切换为列表":"列表课表 · 切换为竖条");
        viewport.setPagingEnabled(page==1);
        int[] tabs={R.id.tab_agenda,R.id.tab_timetable,R.id.tab_mine};for(int i=0;i<tabs.length;i++){TextView t=findViewById(tabs[i]);t.setSelected(i==page);t.setTextColor(getColor(i==page?R.color.brand_navy:R.color.text_secondary));t.setTypeface(null,i==page?Typeface.BOLD:Typeface.NORMAL);}
        if(page==1)renderTimetable(week);else{
            if(page==0&&agendaPage!=null&&!agendaStatus.equals(agendaSignature()))agendaPage=null;
            attach(content,buildSecondary(page));if(page==0)updateAgendaStatus();
        }
        rememberData();requestWarmPages(16);
    }
    private LinearLayout buildSecondary(int target){
        LinearLayout existing=target==0?agendaPage:minePage;if(existing!=null)return existing;
        LinearLayout outer=content,body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);content=body;
        try{if(target==0)renderAgenda();else renderMine();}finally{content=outer;}
        if(target==0)agendaPage=body;else minePage=body;return body;
    }
    private void renderTimetable(int week){
        attach(content,weekBody(selectedDate));weekPane.date=selectedDate;
    }
    private LinearLayout weekBody(LocalDate date){
        LinearLayout cached=weekViews.get(date);if(cached!=null)return cached;
        LinearLayout outer=content,body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);LocalDate previous=selectedDate;
        content=body;selectedDate=date;
        try{buildTimetable(ScheduleRepository.weekNumber(date));}finally{content=outer;selectedDate=previous;}
        weekViews.put(date,body);
        WeekPrefetchPolicy.trim(weekViews,selectedDate);
        return body;
    }
    private void buildTimetable(int week){
        List<AgendaEvent> events=AgendaStore.load(this);
        List<CourseSession> sessions=ScheduleRepository.sessionsInWeek(week);
        if(AppearanceSettings.grid(this)){content.addView(WeekGrid.create(this,selectedDate,sessions,events,this::agendaChanged));return;}
        if(!ScheduleRepository.hasImportedRules()){
            heading("从这里开始");message("还没有可显示的课表。请选择一种方式导入：");
            importActions();
        }
        if(ScheduleRepository.hasImportedRules()&&!ScheduleRepository.isVisibleWeek(week))message("该日期不在"+ScheduleRepository.rangeText()+"课程范围内；仍显示本周个人日程");
        List<Object> items=new ArrayList<>();items.addAll(sessions);LocalDate monday=ScheduleRepository.weekMonday(selectedDate);
        if(events==null)message("日程读取失败，原数据保留。");
        else for(AgendaEvent event:events)if(!event.date.isBefore(monday)&&event.date.isBefore(monday.plusDays(7)))items.add(event);
        items.sort(java.util.Comparator.comparing(this::itemDate).thenComparing(this::itemTime));
        message("左右滑动切换周次 · 点击课程看详情");LocalDate date=null;
        for(Object item:items){LocalDate next=itemDate(item);if(!next.equals(date)){date=next;heading(ScheduleText.dateTitle(date,week));}content.addView(item instanceof CourseSession?courseCard((CourseSession)item,false):eventCard((AgendaEvent)item));}
        if(items.isEmpty())message("本周没有课程或日程");
    }
    private LocalDate itemDate(Object item){return item instanceof CourseSession?((CourseSession)item).getDate():((AgendaEvent)item).date;}
    private void renderAgenda(){
        agendaCountdown=null;
        LocalDate agendaDate=ScheduleRepository.today();
        action("＋ 添加日程",()->AgendaEditor.show(this,null,agendaDate,this::agendaChanged));
        {
            CourseSession next=ScheduleRepository.nextSession(agendaDate,ScheduleRepository.now());
            if(next!=null)agendaCountdown=message(next.getCourse()+" · "+ScheduleRepository.countdownText(next,ScheduleRepository.now()));
        }
        List<CourseSession> classes=ScheduleRepository.sessionsOn(agendaDate);
        agendaClasses=classes;agendaStatus=agendaSignature();
        List<AgendaEvent> stored=AgendaStore.load(this);List<AgendaEvent> events=new ArrayList<>();
        if(stored==null)message("日程数据读取失败，已保留原数据，请勿清除应用数据。");
        if(stored!=null)renderAgendaConflicts(stored);
        List<AgendaEvent> future=new ArrayList<>();
        if(stored!=null)for(AgendaEvent e:stored){if(e.date.isAfter(agendaDate))future.add(e);else events.add(e);}
        events.sort((a,b)->a.start.compareTo(b.start));
        int completed=0,pending=0;
        for(CourseSession s:classes)if(ScheduleRepository.statusAt(s,ScheduleRepository.now())==CourseStatus.ENDED)completed++;else pending++;
        for(AgendaEvent e:events)if(e.done)completed++;else pending++;
        List<Object> items=new ArrayList<>();items.addAll(classes);items.addAll(events);
        items.sort((a,b)->itemTime(a).compareTo(itemTime(b)));
        heading("待完成（"+pending+"）");
        for(Object item:items)if(!itemDone(item))content.addView(item instanceof CourseSession?courseCard((CourseSession)item,false):eventCard((AgendaEvent)item));
        if(pending==0)message("当天没有待完成事项");
        TextView completedTitle=heading("已完成（"+completed+"） "+(expandedCompleted?"⌃":"⌄"));
        completedTitle.setOnClickListener(v->{expandedCompleted=!expandedCompleted;agendaPage=null;render();});
        if(expandedCompleted)for(Object item:items)if(itemDone(item))content.addView(item instanceof CourseSession?courseCard((CourseSession)item,true):eventCard((AgendaEvent)item));
        heading("后续日程（"+future.size()+"）");
        future.sort(java.util.Comparator.comparing((AgendaEvent e)->e.date).thenComparing(e->e.start));
        for(AgendaEvent e:future)content.addView(eventCard(e));
        if(future.isEmpty())message("以后日期的日程会单独放在这里，到当天自动移入待完成。");
    }
    private void renderAgendaConflicts(List<AgendaEvent> events){
        java.util.Map<String,AgendaConflictIndex.Report> conflicts=AgendaConflictIndex.build(events,ScheduleRepository::sessionsOn);
        if(conflicts.isEmpty())return;
        heading("时间冲突（"+conflicts.size()+"）");message("以下未完成的日程与课程或其他日程重叠。点开即可调整；不会自动删除或改动安排。");
        for(AgendaConflictIndex.Report report:conflicts.values()){
            AgendaEvent event=report.event;View card=timeline(event.title,event.date+"\n"+report.description(),event.timeText().replace('–','\n'),"冲突",false);
            GradientDrawable background=EditUi.shape(this,getColor(R.color.card_practice),18);background.setStroke(dp(1),getColor(R.color.action_amber));card.setBackground(background);
            card.setOnClickListener(v->AgendaEditor.show(this,event,event.date,this::agendaChanged));content.addView(card);
        }
    }
    private String agendaSignature(){StringBuilder key=new StringBuilder();java.time.ZonedDateTime now=ScheduleRepository.now();for(CourseSession s:agendaClasses)key.append(ScheduleRepository.statusAt(s,now).ordinal()).append(',');return key.toString();}
    private void updateAgendaStatus(){
        if(!agendaStatus.equals(agendaSignature())){int y=scroll.getScrollY();agendaPage=null;render();scroll.post(()->scroll.scrollTo(0,y));return;}
        if(agendaCountdown!=null){CourseSession next=null;java.time.ZonedDateTime now=ScheduleRepository.now();for(CourseSession s:agendaClasses)if(ScheduleRepository.statusAt(s,now)!=CourseStatus.ENDED){next=s;break;}
            String value=next==null?"今日课程已结束":next.getCourse()+" · "+ScheduleRepository.countdownText(next,now);
            if(!value.contentEquals(agendaCountdown.getText()))agendaCountdown.setText(value);
        }
    }
    private java.time.LocalTime itemTime(Object item){return item instanceof CourseSession?((CourseSession)item).getStartTime():((AgendaEvent)item).allDay?java.time.LocalTime.MIN:((AgendaEvent)item).start;}
    private boolean itemDone(Object item){return item instanceof CourseSession?ScheduleRepository.statusAt((CourseSession)item,ScheduleRepository.now())==CourseStatus.ENDED:((AgendaEvent)item).done;}
    private void renderMine(){
        View summary=getLayoutInflater().inflate(R.layout.view_mine_summary,content,false);
        boolean imported=ScheduleRepository.hasImportedRules();
        ((TextView)summary.findViewById(R.id.mine_status)).setText(imported?"课表已就绪":"等待导入课表");
        long synced=ScheduleRuleStore.lastSyncTime(this);
        ((TextView)summary.findViewById(R.id.mine_status_detail)).setText(imported
                ?ScheduleRepository.rangeText()+" · "+ScheduleRepository.importedRuleCount()+" 个上课安排"
                :"同步教务课表，或导入已下载的 XLS 文件");
        ((TextView)summary.findViewById(R.id.mine_last_import)).setText(synced>0
                ?"最近导入 "+java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM,java.text.DateFormat.SHORT).format(new java.util.Date(synced))
                :"数据保存在本机 · 导入前可预览确认");
        content.addView(summary);
        heading("课表来源");importActions();
        heading("偏好与应用");
        content.addView(MineActionCards.create(this,R.drawable.ic_ui_bell,"上课提醒与闹钟",
                "全部课程、所选课程，或某一次单独提醒","提醒",R.color.action_green,false,()->ReminderSettings.show(this)));
        content.addView(MineActionCards.create(this,R.drawable.ic_ui_file,"导出 / 恢复课表",
                "Excel表格、日历和本地课表备份","数据备份",R.color.brand_navy,false,()->startActivity(new Intent(this,ScheduleTransferActivity.class))));
        content.addView(MineActionCards.create(this,R.drawable.ic_ui_settings,"课表与界面设置",
                "开学时间、显示模式、主题、小组件与提醒","个性化",R.color.action_purple,false,
                ()->startActivity(new Intent(this,SettingsActivity.class))));
        content.addView(MineActionCards.create(this,R.drawable.ic_ui_update,"检查软件更新",
                "打开 App 自动检查，也可点此手动检查","应用更新",R.color.action_amber,false,
                ()->AppUpdates.check(this,true)));
        String version="";
        try{version=" · "+getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
        message(getString(R.string.app_name)+version+"\n没有会员或广告。只有主动获取课表才打开学校登录；软件更新仅在打开 App 时检查。");
    }
    private void importActions(){
        content.addView(MineActionCards.create(this,R.drawable.ic_ui_cloud,"登录并获取课表",
                "打开学校官方登录页，登录后自动获取","教务同步",R.color.brand_navy,true,
                ()->openScheduleImport(false)));
        content.addView(MineActionCards.create(this,R.drawable.ic_ui_file,"导入 XLS 课表",
                "使用学校导出的文件，无需网页登录","本地文件",R.color.action_green,false,
                ()->openScheduleImport(true)));
    }
    private View courseCard(CourseSession s,boolean completed){
        boolean ended=completed||ScheduleRepository.statusAt(s,ScheduleRepository.now())==CourseStatus.ENDED;
        View card=timeline(s.getCourse(),s.getRoom()+" · "+s.getPeriods(),s.getTimeRange().replace('–','\n'),"课程",ended);
        card.setOnClickListener(v->CourseDetailsSheet.show(this,s));return card;
    }
    private View eventCard(AgendaEvent e){View card=timeline(e.title,e.date+" · "+e.place+(e.remind?" · 提醒":""),e.timeText().replace('–','\n'),"日程",e.done);card.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(e.title).setMessage(e.date+" · "+e.timeText()+"\n"+e.place).setPositiveButton(e.done?"恢复待完成":"标记完成",(d,w)->{if(AgendaStore.put(this,e.completed(!e.done),false))agendaChanged();else toast("保存失败，原数据保留");}).setNeutralButton("编辑",(d,w)->AgendaEditor.show(this,e,e.date,this::agendaChanged)).setNegativeButton("关闭",null).show());return card;}
    private View timeline(String title,String details,String time,String type,boolean done){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(18),dp(12),dp(18));
        GradientDrawable bg=new GradientDrawable();bg.setColor(getColor(R.color.surface));bg.setCornerRadius(dp(18));row.setBackground(bg);
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(10);row.setLayoutParams(rp);
        TextView when=text(time,12,R.color.text_secondary);when.setGravity(Gravity.CENTER);row.addView(when,new LinearLayout.LayoutParams(dp(68),-2));
        View stripe=new View(this);stripe.setBackgroundColor(getColor(done?R.color.text_hint:R.color.brand_navy));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(3),dp(48));sp.setMargins(dp(4),0,dp(14),0);row.addView(stripe,sp);
        LinearLayout label=new LinearLayout(this);label.setOrientation(LinearLayout.VERTICAL);TextView name=text(title,18,R.color.text_primary);name.setTypeface(null,Typeface.BOLD);label.addView(name);TextView place=text(details,13,R.color.text_secondary);place.setPadding(0,dp(8),0,0);label.addView(place);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        TextView badge=text(type,11,R.color.text_secondary);badge.setPadding(dp(8),dp(4),0,dp(4));row.addView(badge);if(done){name.setTextColor(getColor(R.color.text_hint));place.setTextColor(getColor(R.color.text_hint));when.setTextColor(getColor(R.color.text_hint));badge.setTextColor(getColor(R.color.text_hint));}row.setContentDescription(title+" "+time+" "+details+(done?" 已完成":""));return row;
    }
    private void agendaChanged(){int y=scroll.getScrollY();invalidatePages();ScheduleChanges.refreshAsync(this);render();scroll.post(()->scroll.scrollTo(0,y));}
    private void action(String value,Runnable run){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setOnClickListener(v->run.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);content.addView(b,p);}
    private TextView heading(String value){TextView t=text(value,20,R.color.text_primary);t.setTypeface(null,Typeface.BOLD);t.setPadding(0,dp(16),0,dp(12));content.addView(t);return t;}
    private TextView message(String value){TextView t=text(value,14,R.color.text_secondary);t.setPadding(0,dp(12),0,dp(12));content.addView(t);return t;}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(getColor(color));return t;}
    private void datePicker(){DatePickerDialog d=new DatePickerDialog(this,(v,y,m,day)->{clearDraft();resetWeekAnimation();selectedDate=LocalDate.of(y,m+1,day);render();scroll.scrollTo(0,0);},selectedDate.getYear(),selectedDate.getMonthValue()-1,selectedDate.getDayOfMonth());d.setTitle("选择具体日期");d.show();}
    private void refreshWidget(){ScheduleChanges.refreshAsync(this);}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);ReminderScheduler.scheduleNext(this);AgendaReminders.schedule(this);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
}
