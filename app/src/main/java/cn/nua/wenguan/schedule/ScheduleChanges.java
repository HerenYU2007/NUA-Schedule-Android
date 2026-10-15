package cn.nua.wenguan.schedule;
import android.content.*;
import android.appwidget.AppWidgetManager;
import cn.nua.wenguan.schedule.widget.TodayScheduleWidgetProvider;
import cn.nua.wenguan.schedule.widget.WeekScheduleWidgetProvider;
public final class ScheduleChanges {
    private static final java.util.concurrent.ExecutorService WORK=java.util.concurrent.Executors.newSingleThreadExecutor();
    private ScheduleChanges() { }
    public static void refreshAsync(Context context){
        Context app=context.getApplicationContext();
        WORK.execute(()->{try{refreshNow(app);}catch(RuntimeException unavailable){/* No UI-thread I/O or private-data logging. */}});
    }
    public static void refresh(Context c){refreshAsync(c);}
    private static void refreshNow(Context c){
        cn.nua.wenguan.schedule.widget.WidgetWork.refresh(c);
        try{ReminderScheduler.scheduleNext(c);}catch(RuntimeException unavailable){}
        try{AgendaReminders.schedule(c);}catch(RuntimeException unavailable){}
    }
}
