package cn.nua.wenguan.schedule;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import cn.nua.wenguan.schedule.model.AgendaEvent;
public final class AgendaReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent) {
        if("AGENDA_REMINDER".equals(intent.getAction())) {
            AgendaEvent next=AgendaReminders.next(c);
            if(next!=null && AgendaReminders.due(next)<=System.currentTimeMillis()+1000) {
                c.getSharedPreferences("agenda_reminders",0).edit().putBoolean(next.id+"|"+AgendaReminders.due(next),true).commit();
                NotificationManager manager=c.getSystemService(NotificationManager.class);
                if(manager!=null&&manager.areNotificationsEnabled()&&(Build.VERSION.SDK_INT<33||c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")==android.content.pm.PackageManager.PERMISSION_GRANTED)) {
                    manager.createNotificationChannel(new NotificationChannel("agenda_reminders","日程提醒",NotificationManager.IMPORTANCE_HIGH));
                    PendingIntent open=PendingIntent.getActivity(c,8429,new Intent(c,MainActivity.class).putExtra("open_agenda",true),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                    manager.notify(next.id.hashCode(),new Notification.Builder(c,"agenda_reminders").setSmallIcon(R.drawable.ic_notification).setContentTitle("日程提醒 · "+next.title).setContentText(next.timeText()+" · "+next.place).setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build());
                }
            }
        }
        AgendaReminders.schedule(c);
    }
}
