package cn.nua.wenguan.schedule.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import cn.nua.wenguan.schedule.data.ScheduleRepository;
import cn.nua.wenguan.schedule.model.CourseSession;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

final class WidgetUpdateScheduler {
    private static final int COURSE_REQUEST_CODE = 2001;
    private static final int TOMORROW_REQUEST_CODE = 2002;
    private static final int RESET_REQUEST_CODE = 2003;

    private WidgetUpdateScheduler() { }

    static void scheduleNext(Context context, List<CourseSession> sessions, ZonedDateTime now) {
        ZonedDateTime next = null;
        for (CourseSession session : sessions) {
            ZonedDateTime start = ZonedDateTime.of(
                    session.getDate(), session.getStartTime(), ScheduleRepository.SCHOOL_ZONE);
            ZonedDateTime ended = ZonedDateTime.of(
                    session.getDate(), session.getEndTime(), ScheduleRepository.SCHOOL_ZONE).plusSeconds(1);
            if (now.isBefore(start) && (next == null || start.isBefore(next))) {
                next = start;
            } else if (now.isBefore(ended) && (next == null || ended.isBefore(next))) {
                next = ended;
            }
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        if (next != null) {
            alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    next.toInstant().toEpochMilli(),
                    60_000L,
                    broadcastIntent(context, COURSE_REQUEST_CODE, TodayScheduleWidgetProvider.ACTION_REFRESH)
            );
        } else {
            alarmManager.cancel(broadcastIntent(
                    context, COURSE_REQUEST_CODE, TodayScheduleWidgetProvider.ACTION_REFRESH));
        }

        ZonedDateTime switchToTomorrow = now.toLocalDate().atTime(22, 0)
                .atZone(ScheduleRepository.SCHOOL_ZONE);
        if (!now.isBefore(switchToTomorrow)) {
            switchToTomorrow = switchToTomorrow.plusDays(1);
        }
        alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                switchToTomorrow.toInstant().toEpochMilli(),
                60_000L,
                broadcastIntent(context, TOMORROW_REQUEST_CODE, TodayScheduleWidgetProvider.ACTION_AUTO_TOMORROW)
        );

        ZonedDateTime resetToToday = now.toLocalDate().plusDays(1).atStartOfDay(ScheduleRepository.SCHOOL_ZONE).plusSeconds(2);
        alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                resetToToday.toInstant().toEpochMilli(),
                60_000L,
                broadcastIntent(context, RESET_REQUEST_CODE, TodayScheduleWidgetProvider.ACTION_RESET_TODAY)
        );
    }

    static void cancel(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(broadcastIntent(
                    context, COURSE_REQUEST_CODE, TodayScheduleWidgetProvider.ACTION_REFRESH));
            alarmManager.cancel(broadcastIntent(
                    context, TOMORROW_REQUEST_CODE, TodayScheduleWidgetProvider.ACTION_AUTO_TOMORROW));
            alarmManager.cancel(broadcastIntent(
                    context, RESET_REQUEST_CODE, TodayScheduleWidgetProvider.ACTION_RESET_TODAY));
        }
    }

    private static PendingIntent broadcastIntent(Context context, int requestCode, String action) {
        Intent intent = new Intent(context, TodayScheduleWidgetProvider.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
