package cn.nua.wenguan.schedule.widget;
import android.app.job.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.os.PersistableBundle;
import cn.nua.wenguan.schedule.data.*;
import java.util.concurrent.*;

/** Local widget reconstruction only. No WebView, login, network fetch or course deletion. */
public final class WidgetRecovery {
    private static final int JOB=8236;
    private WidgetRecovery() { }
    public static boolean hasWidgets(Context c){return widgetCount(c)>0;}
    private static int widgetCount(Context c){
        int count=0;
        AppWidgetManager m=AppWidgetManager.getInstance(c);
        for(Class<?> type:new Class<?>[]{TodayScheduleWidgetProvider.class,WhiteTodayScheduleWidgetProvider.class,WeekScheduleWidgetProvider.class,WhiteWeekScheduleWidgetProvider.class})
            count+=m.getAppWidgetIds(new ComponentName(c,type)).length;
        return count;
    }
    public static void cancel(Context c){JobScheduler scheduler=c.getSystemService(JobScheduler.class);if(scheduler!=null)for(int attempt=1;attempt<=WidgetRecoveryPolicy.ATTEMPTS;attempt++)scheduler.cancel(JOB+attempt);}
    private static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("widget_recovery",Context.MODE_PRIVATE);}
    private static long installedAt(Context c){try{return c.getPackageManager().getPackageInfo(c.getPackageName(),0).lastUpdateTime;}catch(android.content.pm.PackageManager.NameNotFoundException absent){return 0;}}
    public static void ensureAfterUpgrade(Context c){
        try{long installed=installedAt(c);
            if(installed>0&&prefs(c).getLong("seen_update",0)!=installed){
                if(hasWidgets(c))begin(c,null);else prefs(c).edit().putLong("seen_update",installed).apply();
            }
        }catch(RuntimeException unavailable){/* A launcher/service failure must never crash opening the app. */}
    }
    public static synchronized void begin(Context context,Runnable finished){
        Context c=context.getApplicationContext();long generation=prefs(c).getLong("generation",0)+1;
        prefs(c).edit().putLong("generation",generation).apply();
        try{cancel(c);schedule(c,generation,1);}catch(RuntimeException unavailable){/* Immediate reconstruction still runs. */}
        WidgetWork.enqueue(c,"all-widgets",()->{if(prefs(c).getLong("generation",0)==generation&&refreshNow(c))prefs(c).edit().putLong("seen_update",installedAt(c)).apply();},finished);
    }
    private static void schedule(Context c,long generation,int attempt){
        if(!WidgetRecoveryPolicy.current(generation,prefs(c).getLong("generation",0),attempt))return;
        PersistableBundle extras=new PersistableBundle();extras.putLong("generation",generation);extras.putInt("attempt",attempt);
        JobScheduler scheduler=c.getSystemService(JobScheduler.class);
        if(scheduler!=null)scheduler.schedule(new JobInfo.Builder(JOB+attempt,new ComponentName(c,WidgetRecoveryJobService.class))
                .setMinimumLatency(WidgetRecoveryPolicy.delay(attempt)).setOverrideDeadline(WidgetRecoveryPolicy.delay(attempt)+15000).setExtras(extras).build());
    }
    static Future<?> redraw(Context context,long generation,int attempt,Runnable finished){
        Context c=context.getApplicationContext();
        FutureTask<Void> task=new FutureTask<>(()->{
            if(WidgetRecoveryPolicy.current(generation,prefs(c).getLong("generation",0),attempt)&&!Thread.currentThread().isInterrupted()&&refreshNow(c))prefs(c).edit().putLong("seen_update",installedAt(c)).apply();
            return null;
        });
        WidgetWork.enqueue(c,"recovery-job-"+attempt,task,finished);return task;
    }

    static void next(Context c,long generation,int attempt){
        if(attempt<WidgetRecoveryPolicy.ATTEMPTS)try{schedule(c,generation,attempt+1);}catch(RuntimeException unavailable){}
    }
    public static boolean refreshNow(Context c){
        long successes=WidgetDiagnostics.accepted();
        if(ScheduleRuleStore.readRules(c)==null){WidgetDiagnostics.failure(c,"saved-data",-1,new IllegalStateException());return false;} // Transient/unreadable data must not be presented as an empty import.
        ScheduleRuleStore.applySaved(c);
        try{TodayScheduleWidgetProvider.refreshAll(c);}catch(RuntimeException unavailable){WidgetDiagnostics.failure(c,"TodayScheduleWidgetProvider",-1,unavailable);}
        try{WhiteTodayScheduleWidgetProvider.refreshAll(c);}catch(RuntimeException unavailable){WidgetDiagnostics.failure(c,"WhiteTodayScheduleWidgetProvider",-1,unavailable);}
        try{WeekScheduleWidgetProvider.refreshAll(c);}catch(RuntimeException unavailable){WidgetDiagnostics.failure(c,"WeekScheduleWidgetProvider",-1,unavailable);}
        try{WhiteWeekScheduleWidgetProvider.refreshAll(c);}catch(RuntimeException unavailable){WidgetDiagnostics.failure(c,"WhiteWeekScheduleWidgetProvider",-1,unavailable);}
        boolean submitted=WidgetDiagnostics.accepted()-successes>=widgetCount(c);
        if(submitted)prefs(c).edit().putLong("seen_update",installedAt(c)).apply();
        return submitted;
    }
}
