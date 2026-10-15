package cn.nua.wenguan.schedule.widget;

import android.appwidget.AppWidgetManager;
import android.content.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Bounded local status only: never exception messages, course text, account or login data. */
public final class WidgetDiagnostics {
    private static final java.util.concurrent.atomic.AtomicLong ACCEPTED=new java.util.concurrent.atomic.AtomicLong();
    public static long accepted(){return ACCEPTED.get();}
    private static final String PREFS="widget_diagnostics";
    private WidgetDiagnostics() { }
    public static void failure(Context c,String stage,int id,RuntimeException error){record(c,stage,id,"失败："+error.getClass().getSimpleName());}
    public static void submitted(Context c,String stage,int id){ACCEPTED.incrementAndGet();record(c,stage,id,"已提交给桌面");}
    private static synchronized void record(Context c,String stage,int id,String outcome){
        android.content.SharedPreferences p=c.getSharedPreferences(PREFS,0);
        String line=LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+" · "+stage+(id<0?"":" #"+id)+" · "+outcome;
        String old=p.getString("events","");String[] entries=(line+"\n"+old).split("\n");
        StringBuilder result=new StringBuilder();for(int n=0;n<Math.min(24,entries.length);n++)result.append(entries[n]).append('\n');
        p.edit().putString("events",result.toString()).apply();
    }
    public static String summary(Context c){
        StringBuilder result=new StringBuilder("“已提交”表示系统已接收刷新，不代表已确认桌面显示。\n\n");
        Class<?>[] types={TodayScheduleWidgetProvider.class,WhiteTodayScheduleWidgetProvider.class,WeekScheduleWidgetProvider.class,WhiteWeekScheduleWidgetProvider.class};
        String[] names={"今日深色","今日白色","周表深色","周表白色"};AppWidgetManager manager=AppWidgetManager.getInstance(c);
        for(int n=0;n<types.length;n++)result.append(names[n]).append("：").append(manager.getAppWidgetIds(new ComponentName(c,types[n])).length).append("个\n");
        return result+"\n"+c.getSharedPreferences(PREFS,0).getString("events","尚无刷新记录\n")+"\n仅本机状态与错误类型，不含课表和登录信息。";
    }
}
