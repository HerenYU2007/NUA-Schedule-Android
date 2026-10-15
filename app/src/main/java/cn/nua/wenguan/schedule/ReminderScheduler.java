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
        return s.getDate() + "|" + s.getStartTime() + "|" + s.getEndTime() + "|" + (s.getRuleId().isEmpty()?s.getCourse():s.getRuleId());
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
        c.getSharedPreferences("reminder_retry",0).edit().remove(retryKey(s)).remove(retryKey(s)+"_count").apply();
    }
    public static CourseSession next(Context c) {
        java.util.List<CourseSession> candidates=candidates(c);
        return candidates.isEmpty()?null:candidates.get(0);
    }
    public static long due(Context c,CourseSession s){return ZonedDateTime.of(s.getDate(),s.getStartTime(),ScheduleRepository.SCHOOL_ZONE).minusMinutes(ReminderPreferences.minutes(c,s)).toInstant().toEpochMilli();}
    public static java.util.List<CourseSession> candidates(Context c) {
        if(!enabled(c)||!NotificationAccess.allowed(c))return java.util.Collections.emptyList();
        ScheduleRuleStore.applySaved(c);
        Set<String> keys = fired(c);
        ZonedDateTime now = ScheduleRepository.now();
        java.util.List<CourseSession> result=new java.util.ArrayList<>();
        boolean exact=exactAllowed(c),notifications=NotificationAccess.channelAllowed(c,NotificationAccess.CLASSES),alarms=NotificationAccess.channelAllowed(c,NotificationAccess.ALARM);
        if(!notifications&&!alarms)return result;
        long instant=now.toInstant().toEpochMilli();
        for (int day = 0; day < 366; day++) {
            LocalDate date = now.toLocalDate().plusDays(day);
            for (CourseSession s : ScheduleRepository.sessionsOn(date)) {
                int mode=ReminderPreferences.mode(c,s);
                long end=ZonedDateTime.of(s.getDate(),s.getEndTime(),ScheduleRepository.SCHOOL_ZONE).toInstant().toEpochMilli();
                if ((mode==2&&exact?alarms:notifications) && ReminderPolicy.eligible(instant,end,due(c,s),mode==2&&!exact?1:mode,keys.contains(identity(s)))) result.add(s);
            }
            if(!result.isEmpty()){
                long earliest=Long.MAX_VALUE;for(CourseSession s:result)earliest=Math.min(earliest,scheduledDue(c,s));
                long nextDayEarliest=date.plusDays(1).atStartOfDay(ScheduleRepository.SCHOOL_ZONE).minusMinutes(120).toInstant().toEpochMilli();
                if(nextDayEarliest>earliest)break;
            }
        }
        result.sort(java.util.Comparator.comparingLong(s->scheduledDue(c,s)));
        return result;
    }
    private static String retryKey(CourseSession s){return java.util.UUID.nameUUIDFromBytes(identity(s).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();}
    public static long scheduledDue(Context c,CourseSession s){return Math.max(due(c,s),c.getSharedPreferences("reminder_retry",0).getLong(retryKey(s),0));}
    public static void failed(Context c,CourseSession s){android.content.SharedPreferences p=c.getSharedPreferences("reminder_retry",0);String key=retryKey(s);int count=p.getInt(key+"_count",0)+1;long retry=count<2?System.currentTimeMillis()+60000:ZonedDateTime.of(s.getDate(),s.getEndTime(),ScheduleRepository.SCHOOL_ZONE).toInstant().toEpochMilli()+1000;p.edit().putInt(key+"_count",count).putLong(key,retry).apply();}
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
        NotificationAccess.channels(c);
        CourseSession s = next(c);
        if (s == null) return;
        long at = Math.max(System.currentTimeMillis() + 1500L,
                scheduledDue(c,s));
        try {
            if (exactAllowed(c) && ReminderPreferences.mode(c,s)==2) {
                PendingIntent show=PendingIntent.getActivity(c,7231,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                a.setAlarmClock(new AlarmManager.AlarmClockInfo(at,show),p);
            }
            else if (exactAllowed(c)) a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
            else a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
        } catch (SecurityException denied) { a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p); }
    }
}
