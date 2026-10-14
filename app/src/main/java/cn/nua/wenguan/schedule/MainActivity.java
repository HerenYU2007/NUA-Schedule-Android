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
    private final Handler ticker=new Handler(Looper.getMainLooper());
    private final Runnable tick=new Runnable(){@Override public void run(){if(!isFinishing()){
        if(page==0||!ScheduleRepository.today().equals(renderedToday))render();
        ticker.postDelayed(this,30000);
    }}};
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); LegacySyncCleanup.cancel(this); setContentView(R.layout.activity_main);
        ScheduleRuleStore.applySaved(this);
        selectedDate=state!=null&&state.containsKey("selected_date")?LocalDate.ofEpochDay(state.getLong("selected_date")):ScheduleRepository.today();
        page=state==null?(getIntent().getBooleanExtra("open_agenda",false)?0:1):state.getInt("page",1);
        emptyPromptHandled=state!=null&&state.getBoolean("empty_prompt_handled",false);
        content=findViewById(R.id.page_content);scroll=findViewById(R.id.page_scroll);
        findViewById(R.id.tab_agenda).setOnClickListener(v->selectPage(0));
        findViewById(R.id.tab_timetable).setOnClickListener(v->selectPage(1));
        findViewById(R.id.tab_mine).setOnClickListener(v->selectPage(2));
        findViewById(R.id.previous_week).setOnClickListener(v->changeWeek(-1));
        findViewById(R.id.next_week).setOnClickListener(v->changeWeek(1));
        findViewById(R.id.select_date).setOnClickListener(v->datePicker());
        findViewById(R.id.page_date).setOnClickListener(v->{if(page!=2)datePicker();});
        findViewById(R.id.display_mode).setOnClickListener(v->{if(AppearanceSettings.saveGrid(this,!AppearanceSettings.grid(this)))render();else toast("显示模式保存失败");});
        render();
    }
    // Cold launch and reopening the main screen: network work remains off the UI thread.
    @Override protected void onStart(){super.onStart();AppUpdates.checkOnOpen(this);}
    private void selectPage(int selected){page=selected;scroll.scrollTo(0,0);render();}
    private void changeWeek(int direction){selectedDate=selectedDate.plusWeeks(direction);render();scroll.scrollTo(0,0);content.setAlpha(.4f);content.animate().alpha(1).setDuration(180).start();}
    @Override protected void onResume(){super.onResume();openingImport=false;ScheduleRuleStore.applySaved(this);if(content!=null)render();refreshWidget();AgendaReminders.schedule(this);AppUpdates.resumeInstall(this);ticker.removeCallbacks(tick);ticker.postDelayed(tick,30000);}
    @Override protected void onPause(){ticker.removeCallbacks(tick);super.onPause();}
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
        renderedToday=ScheduleRepository.today();content.removeAllViews();
        ((TextView)findViewById(R.id.page_date)).setText(page==2?"我的":selectedDate.toString().replace('-','/')+" 周"+"一二三四五六日".charAt(selectedDate.getDayOfWeek().getValue()-1));
        int week=ScheduleRepository.weekNumber(selectedDate);
        ((TextView)findViewById(R.id.page_subtitle)).setText(page==1?(week>0?"第"+week+"周 · ":"")+(AppearanceSettings.grid(this)?"竖条课表":"列表课表"):page==0?"当天课程与个人事项":"南艺电子课表 · 数据保存在本机");
        findViewById(R.id.week_controls).setVisibility(page==1?View.VISIBLE:View.GONE);
        findViewById(R.id.display_mode).setVisibility(page==1?View.VISIBLE:View.GONE);
        ((TextView)findViewById(R.id.display_mode)).setText(AppearanceSettings.grid(this)?"竖条课表 · 切换为列表":"列表课表 · 切换为竖条");
        scroll.setOnWeekSwipe(page==1?this::changeWeek:null);
        int[] tabs={R.id.tab_agenda,R.id.tab_timetable,R.id.tab_mine};for(int i=0;i<tabs.length;i++){TextView t=findViewById(tabs[i]);t.setSelected(i==page);t.setTextColor(getColor(i==page?R.color.brand_navy:R.color.text_secondary));t.setTypeface(null,i==page?Typeface.BOLD:Typeface.NORMAL);}
        if(page==1)renderTimetable(week);else if(page==0)renderAgenda();else renderMine();
    }
    private void renderTimetable(int week){
        if(!ScheduleRepository.hasImportedRules()){
            heading("从这里开始");message("还没有可显示的课表。请选择一种方式导入：");
            importActions();return;
        }
        if(!ScheduleRepository.isVisibleWeek(week)){message("该日期不在"+ScheduleRepository.rangeText()+"范围内");return;}
        List<CourseSession> sessions=ScheduleRepository.sessionsInWeek(week);
        if(AppearanceSettings.grid(this)){content.addView(WeekGrid.create(this,selectedDate,sessions));return;}
        message("左右滑动切换周次 · 点击课程看详情");LocalDate date=null;
        for(CourseSession s:sessions){if(!s.getDate().equals(date)){date=s.getDate();heading(ScheduleText.dateTitle(date,week));}content.addView(courseCard(s,false));}
        if(sessions.isEmpty())message("本周没有课程");
    }
    private void renderAgenda(){
        action("＋ 添加日程",()->AgendaEditor.show(this,null,selectedDate,this::agendaChanged));
        if(selectedDate.equals(ScheduleRepository.today())) {
            CourseSession next=ScheduleRepository.nextSession(selectedDate,ScheduleRepository.now());
            if(next!=null)message(next.getCourse()+" · "+ScheduleRepository.countdownText(next,ScheduleRepository.now()));
        }
        List<CourseSession> classes=ScheduleRepository.sessionsOn(selectedDate);
        List<AgendaEvent> stored=AgendaStore.load(this);List<AgendaEvent> events=new ArrayList<>();
        if(stored==null)message("日程数据读取失败，已保留原数据，请勿清除应用数据。");
        else for(AgendaEvent e:stored)if(e.date.equals(selectedDate))events.add(e);
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
        completedTitle.setOnClickListener(v->{expandedCompleted=!expandedCompleted;render();});
        if(expandedCompleted)for(Object item:items)if(itemDone(item))content.addView(item instanceof CourseSession?courseCard((CourseSession)item,true):eventCard((AgendaEvent)item));
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
        content.addView(MineActionCards.create(this,R.drawable.ic_ui_settings,"课表与界面设置",
                "开学时间、显示模式、主题、小组件与提醒","个性化",R.color.action_purple,false,
                ()->startActivity(new Intent(this,SettingsActivity.class))));
        content.addView(MineActionCards.create(this,R.drawable.ic_ui_update,"检查软件更新",
                "打开 App 自动检查，也可点此手动检查","应用更新",R.color.action_amber,false,
                ()->AppUpdates.check(this,true)));
        String version="";
        try{version=" · "+getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
        message("南艺电子课表"+version+"\n没有会员或广告。只有主动获取课表才打开学校登录；软件更新仅在打开 App 时检查。");
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
    private View eventCard(AgendaEvent e){View card=timeline(e.title,e.place+(e.remind?" · 提醒":""),e.timeText().replace('–','\n'),"日程",e.done);card.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(e.title).setMessage(e.date+" · "+e.timeText()+"\n"+e.place).setPositiveButton(e.done?"恢复待完成":"标记完成",(d,w)->{if(AgendaStore.put(this,e.completed(!e.done),false))agendaChanged();else toast("保存失败，原数据保留");}).setNeutralButton("编辑",(d,w)->AgendaEditor.show(this,e,e.date,this::agendaChanged)).setNegativeButton("关闭",null).show());return card;}
    private View timeline(String title,String details,String time,String type,boolean done){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(18),dp(12),dp(18));
        GradientDrawable bg=new GradientDrawable();bg.setColor(getColor(R.color.surface));bg.setCornerRadius(dp(18));row.setBackground(bg);
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(10);row.setLayoutParams(rp);
        TextView when=text(time,12,R.color.text_secondary);when.setGravity(Gravity.CENTER);row.addView(when,new LinearLayout.LayoutParams(dp(68),-2));
        View stripe=new View(this);stripe.setBackgroundColor(getColor(done?R.color.text_hint:R.color.brand_navy));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(3),dp(48));sp.setMargins(dp(4),0,dp(14),0);row.addView(stripe,sp);
        LinearLayout label=new LinearLayout(this);label.setOrientation(LinearLayout.VERTICAL);TextView name=text(title,18,R.color.text_primary);name.setTypeface(null,Typeface.BOLD);label.addView(name);TextView place=text(details,13,R.color.text_secondary);place.setPadding(0,dp(8),0,0);label.addView(place);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        TextView badge=text(type,11,R.color.text_secondary);badge.setPadding(dp(8),dp(4),0,dp(4));row.addView(badge);if(done)row.setAlpha(.55f);row.setContentDescription(title+" "+time+" "+details+(done?" 已完成":""));return row;
    }
    private void agendaChanged(){AgendaReminders.schedule(this);render();}
    private void action(String value,Runnable run){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setOnClickListener(v->run.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);content.addView(b,p);}
    private TextView heading(String value){TextView t=text(value,20,R.color.text_primary);t.setTypeface(null,Typeface.BOLD);t.setPadding(0,dp(16),0,dp(12));content.addView(t);return t;}
    private void message(String value){TextView t=text(value,14,R.color.text_secondary);t.setPadding(0,dp(12),0,dp(12));content.addView(t);}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(getColor(color));return t;}
    private void datePicker(){DatePickerDialog d=new DatePickerDialog(this,(v,y,m,day)->{selectedDate=LocalDate.of(y,m+1,day);render();scroll.scrollTo(0,0);},selectedDate.getYear(),selectedDate.getMonthValue()-1,selectedDate.getDayOfMonth());d.setTitle("选择具体日期");d.show();}
    private void refreshWidget(){try{ReminderScheduler.scheduleNext(this);}catch(RuntimeException ignored){}AppWidgetManager m=AppWidgetManager.getInstance(this);Intent i=new Intent(this,TodayScheduleWidgetProvider.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,m.getAppWidgetIds(new ComponentName(this,TodayScheduleWidgetProvider.class)));sendBroadcast(i);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
}
