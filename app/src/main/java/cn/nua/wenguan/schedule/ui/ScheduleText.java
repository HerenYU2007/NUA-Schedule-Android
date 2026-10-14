package cn.nua.wenguan.schedule.ui;

import cn.nua.wenguan.schedule.model.CourseSession;
import cn.nua.wenguan.schedule.model.CourseStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class ScheduleText {
    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA);
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA);

    private ScheduleText() { }

    public static String weekday(DayOfWeek day) {
        switch (day) {
            case MONDAY: return "周一";
            case TUESDAY: return "周二";
            case WEDNESDAY: return "周三";
            case THURSDAY: return "周四";
            case FRIDAY: return "周五";
            case SATURDAY: return "周六";
            case SUNDAY: return "周日";
            default: return "";
        }
    }

    public static String dateTitle(LocalDate date, int week) {
        String weekText = week > 0 ? " · 第" + week + "周" : "";
        return FULL_DATE.format(date) + " " + weekday(date.getDayOfWeek()) + weekText;
    }

    public static String shortDateTitle(LocalDate date, int week) {
        String weekText = week > 0 ? " · 第" + week + "周" : "";
        return MONTH_DAY.format(date) + " " + weekday(date.getDayOfWeek()) + weekText;
    }

    public static String courseLine(CourseSession session) {
        return session.getTimeRange() + "  " + session.getCourse();
    }

    public static String roomLine(CourseSession session) {
        return session.getRoom() + " · " + session.getType() + " · " + session.getPeriods();
    }

    public static String widgetCourse(CourseSession session, CourseStatus status) {
        return status.getLabel() + " · " + courseLine(session) + "\n" + session.getRoom() + " · " + session.getType();
    }

    public static String countText(List<CourseSession> sessions) {
        return sessions.isEmpty() ? "今天没有课" : "共 " + sessions.size() + " 节课";
    }
}
