package cn.nua.wenguan.schedule;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import cn.nua.wenguan.schedule.data.ScheduleRepository;
import cn.nua.wenguan.schedule.model.CourseSession;
import java.time.ZonedDateTime;

public final class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction()) || Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) LegacySyncCleanup.cancel(c);
        if (!"CLASS_REMINDER".equals(i.getAction())) {
            ReminderScheduler.scheduleNext(c);
            return;
        }
        if (!ReminderScheduler.enabled(c)) return;
        CourseSession s = ReminderScheduler.next(c);
        if (s == null) return;
        long due = ZonedDateTime.of(s.getDate(), s.getStartTime(), ScheduleRepository.SCHOOL_ZONE)
                .minusMinutes(ReminderScheduler.minutesBefore(c)).toInstant().toEpochMilli();
        if (due > System.currentTimeMillis() + 1000L) { ReminderScheduler.scheduleNext(c); return; }
        ReminderScheduler.markFired(c, s);
        NotificationManager m = c.getSystemService(NotificationManager.class);
        if (m != null && m.areNotificationsEnabled()
                && (Build.VERSION.SDK_INT < 33 || c.checkSelfPermission("android.permission.POST_NOTIFICATIONS") == android.content.pm.PackageManager.PERMISSION_GRANTED)) {
            m.createNotificationChannel(new NotificationChannel("class_reminders", "上课提醒", NotificationManager.IMPORTANCE_HIGH));
            PendingIntent open = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            m.notify(ReminderScheduler.identity(s).hashCode(), new Notification.Builder(c, "class_reminders")
                    .setSmallIcon(R.drawable.ic_notification).setContentTitle("即将上课 · " + s.getCourse())
                    .setContentText(s.getTimeRange() + " · " + s.getRoom())
                    .setStyle(new Notification.BigTextStyle().bigText(s.getTimeRange() + " · " + s.getRoom()))
                    .setVisibility(Notification.VISIBILITY_PRIVATE).setAutoCancel(true).setContentIntent(open).build());
        }
        ReminderScheduler.scheduleNext(c);
    }
}
