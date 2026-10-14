package cn.nua.wenguan.schedule;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import cn.nua.wenguan.schedule.data.ScheduleRepository;
import cn.nua.wenguan.schedule.data.ScheduleRuleStore;
import cn.nua.wenguan.schedule.model.CourseSession;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/** One next alarm; fired identities prevent duplicate reminders on refresh/reboot. */
public final class ReminderScheduler {
    private ReminderScheduler() { }
    public static boolean enabled(Context c) { return c.getSharedPreferences("reminders", 0).getBoolean("enabled", false); }
    public static int minutesBefore(Context c) { return c.getSharedPreferences("reminders", 0).getInt("minutes", 15); }
    public static void configure(Context c, boolean enabled, int minutes) {
        c.getSharedPreferences("reminders", 0).edit().putBoolean("enabled", enabled).putInt("minutes", minutes).apply();
        scheduleNext(c);
    }
    public static String identity(CourseSession s) {
        return s.getDate() + "|" + s.getStartTime() + "|" + s.getEndTime() + "|" + s.getCourse();
    }
    private static Set<String> fired(Context c) {
        return new HashSet<>(c.getSharedPreferences("reminders", 0).getStringSet("fired", new HashSet<>()));
    }
    public static void markFired(Context c, CourseSession s) {
        Set<String> keys = fired(c);
        String today = ScheduleRepository.today().toString();
        keys.removeIf(k -> k.length() < 10 || k.substring(0, 10).compareTo(today) < 0);
        keys.add(identity(s));
        c.getSharedPreferences("reminders", 0).edit().putStringSet("fired", keys).apply();
    }
    public static CourseSession next(Context c) {
        ScheduleRuleStore.applySaved(c);
        Set<String> keys = fired(c);
        ZonedDateTime now = ScheduleRepository.now();
        for (int day = 0; day < 366; day++) {
            LocalDate date = now.toLocalDate().plusDays(day);
            for (CourseSession s : ScheduleRepository.sessionsOn(date)) {
                ZonedDateTime start = ZonedDateTime.of(s.getDate(), s.getStartTime(), ScheduleRepository.SCHOOL_ZONE);
                if (start.isAfter(now) && !keys.contains(identity(s))) return s;
            }
        }
        return null;
    }
    public static boolean exactAllowed(Context c) {
        AlarmManager a = c.getSystemService(AlarmManager.class);
        return a != null && (Build.VERSION.SDK_INT < 31 || a.canScheduleExactAlarms());
    }
    public static void scheduleNext(Context c) {
        AlarmManager a = c.getSystemService(AlarmManager.class);
        if (a == null) return;
        PendingIntent p = PendingIntent.getBroadcast(c, 7231, new Intent(c, ReminderReceiver.class).setAction("CLASS_REMINDER"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        a.cancel(p);
        if (!enabled(c)) return;
        CourseSession s = next(c);
        if (s == null) return;
        long at = Math.max(System.currentTimeMillis() + 1500L,
                ZonedDateTime.of(s.getDate(), s.getStartTime(), ScheduleRepository.SCHOOL_ZONE)
                        .minusMinutes(minutesBefore(c)).toInstant().toEpochMilli());
        try {
            if (exactAllowed(c)) a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
            else a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
        } catch (SecurityException denied) { a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p); }
    }
}
