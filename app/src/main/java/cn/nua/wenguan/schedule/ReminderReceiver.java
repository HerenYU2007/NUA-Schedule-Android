package cn.nua.wenguan.schedule;
import android.content.*;
import cn.nua.wenguan.schedule.model.CourseSession;
public final class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        if(Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction())||Intent.ACTION_BOOT_COMPLETED.equals(i.getAction()))LegacySyncCleanup.cancel(c);
        if("CLASS_REMINDER".equals(i.getAction())&&ReminderScheduler.enabled(c)){
            java.util.List<CourseSession> due=new java.util.ArrayList<>();java.util.List<String> titles=new java.util.ArrayList<>();
            for(CourseSession s:ReminderScheduler.candidates(c)){if(ReminderScheduler.scheduledDue(c,s)>System.currentTimeMillis()+1000L)break;due.add(s);if(ReminderPreferences.mode(c,s)==2)titles.add(s.getCourse()+" · "+s.getTimeRange()+" · "+s.getRoom());}
            boolean alarmStarted=false;
            if(!titles.isEmpty()&&ReminderScheduler.exactAllowed(c))try{c.startForegroundService(new Intent(c,CourseAlarmService.class).putExtra("title",titles.size()>1?"同时有"+titles.size()+"门课程":due.stream().filter(s->ReminderPreferences.mode(c,s)==2).findFirst().get().getCourse()).putExtra("body",String.join("\n",titles)));alarmStarted=true;}catch(RuntimeException denied){}
            for(CourseSession s:due){
                int mode=ReminderPreferences.mode(c,s);boolean sent=false;
                if(mode==2&&alarmStarted)sent=true;
                if(!sent)sent=NotificationAccess.post(c,NotificationAccess.CLASSES,ReminderScheduler.identity(s).hashCode(),"即将上课 · "+s.getCourse(),s.getTimeRange()+" · "+s.getRoom()+(mode==2&&!ReminderScheduler.exactAllowed(c)?"（未授权准时闹钟，改为通知）":""),false);
                if(sent)ReminderScheduler.markFired(c,s);else ReminderScheduler.failed(c,s);
            }
        }
        ReminderScheduler.scheduleNext(c);
    }
}
