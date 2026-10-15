package cn.nua.wenguan.schedule;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import cn.nua.wenguan.schedule.data.AgendaStore;
import cn.nua.wenguan.schedule.model.AgendaEvent;
import java.time.ZonedDateTime;
import java.util.List;
import cn.nua.wenguan.schedule.data.ScheduleRepository;
public final class AgendaReminders {
    private AgendaReminders() { }
    public static long due(AgendaEvent e) { return ZonedDateTime.of(e.date,e.start,ScheduleRepository.SCHOOL_ZONE).toInstant().toEpochMilli()-(e.allDay?0:15*60*1000); }
    public static AgendaEvent next(Context c) {
        List<AgendaEvent> events=AgendaStore.load(c); if(events==null)return null;
        AgendaEvent result=null; long now=System.currentTimeMillis();
        for(AgendaEvent e:events) if(e.remind&&!e.done&&due(e)>now-30*60*1000L
                && !c.getSharedPreferences("agenda_reminders",0).getBoolean(e.id+"|"+due(e),false)) {
            if(result==null||due(e)<due(result))result=e;
        }
        return result;
    }
    public static void schedule(Context c) {
        try {
            AlarmManager alarms=c.getSystemService(AlarmManager.class); if(alarms==null)return;
            PendingIntent pending=PendingIntent.getBroadcast(c,8429,new Intent(c,AgendaReminderReceiver.class).setAction("AGENDA_REMINDER"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            alarms.cancel(pending); NotificationAccess.channels(c);if(!NotificationAccess.channelAllowed(c,NotificationAccess.AGENDA))return; AgendaEvent next=next(c); if(next==null)return;
            long at=Math.max(System.currentTimeMillis()+1500,due(next));
            if(Build.VERSION.SDK_INT<31||alarms.canScheduleExactAlarms()) {
                try { alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pending); return; } catch(SecurityException denied) { }
            }
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pending);
        } catch(RuntimeException ignored) { }
    }
}
