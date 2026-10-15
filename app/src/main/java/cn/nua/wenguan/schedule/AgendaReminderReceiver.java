package cn.nua.wenguan.schedule;
import android.content.*;
import cn.nua.wenguan.schedule.model.AgendaEvent;
public final class AgendaReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent){
        if("AGENDA_REMINDER".equals(intent.getAction()))for(int n=0;n<100;n++){
            AgendaEvent next=AgendaReminders.next(c);if(next==null||AgendaReminders.due(next)>System.currentTimeMillis()+1000)break;
            if(!NotificationAccess.post(c,NotificationAccess.AGENDA,next.id.hashCode(),"日程提醒 · "+next.title,next.timeText()+" · "+next.place,true))break;
            if(!c.getSharedPreferences("agenda_reminders",0).edit().putBoolean(next.id+"|"+AgendaReminders.due(next),true).commit())break;
        }
        AgendaReminders.schedule(c);
    }
}
