package cn.nua.wenguan.schedule.data;

import cn.nua.wenguan.schedule.model.CourseSession;
import cn.nua.wenguan.schedule.model.CourseStatus;
import cn.nua.wenguan.schedule.model.ScheduleRule;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ScheduleRepository {
    public static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Shanghai");
    private static volatile List<ScheduleRule> importedRules = Collections.emptyList();
    private static volatile LocalDate importedStartDate = today();

    private ScheduleRepository() { }
    public static LocalDate today() { return LocalDate.now(SCHOOL_ZONE); }
    public static ZonedDateTime now() { return ZonedDateTime.now(SCHOOL_ZONE); }
    public static LocalDate defaultWeekOneMonday() {
        return weekMonday(today());
    }
    public static LocalDate weekMonday(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
    public static String firstWeekText(LocalDate startDate) {
        LocalDate monday = weekMonday(startDate);
        return "第1周：" + monday + " 至 " + monday.plusDays(6) + "（周一至周日）";
    }

    public static CourseStatus statusAt(CourseSession session, ZonedDateTime now) {
        ZonedDateTime schoolNow = now.withZoneSameInstant(SCHOOL_ZONE);
        ZonedDateTime start = ZonedDateTime.of(session.getDate(), session.getStartTime(), SCHOOL_ZONE);
        ZonedDateTime end = ZonedDateTime.of(session.getDate(), session.getEndTime(), SCHOOL_ZONE);
        if (schoolNow.isBefore(start)) return CourseStatus.UPCOMING;
        if (!schoolNow.isBefore(end)) return CourseStatus.ENDED;
        return CourseStatus.IN_PROGRESS;
    }

    public static CourseSession nextSession(LocalDate date, ZonedDateTime now) {
        for (CourseSession session : sessionsOn(date)) {
            if (statusAt(session, now) != CourseStatus.ENDED) return session;
        }
        return null;
    }

    public static String countdownText(CourseSession session, ZonedDateTime now) {
        if (session == null) return "今日没有未结束课程";
        CourseStatus status = statusAt(session, now);
        ZonedDateTime target = status == CourseStatus.IN_PROGRESS
                ? ZonedDateTime.of(session.getDate(), session.getEndTime(), SCHOOL_ZONE)
                : ZonedDateTime.of(session.getDate(), session.getStartTime(), SCHOOL_ZONE);
        long minutes = Math.max(0L, Duration.between(now.withZoneSameInstant(SCHOOL_ZONE), target).toMinutes());
        long hours = minutes / 60L;
        long rest = minutes % 60L;
        String remaining = hours > 0 ? hours + "小时" + rest + "分钟" : rest + "分钟";
        return status == CourseStatus.IN_PROGRESS ? "正在上课 · 还剩 " + remaining : "还有 " + remaining + " 上课";
    }

    // Unconfigured schedules have no semester week; the UI hides week labels.
    public static int weekNumber(LocalDate date) {
        if (!hasImportedRules()) return 0;
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return (int) ChronoUnit.WEEKS.between(weekOneMonday(), monday) + 1;
    }
    public static LocalDate mondayOfWeek(int week) { return weekOneMonday().plusWeeks(week - 1L); }

    public static List<CourseSession> sessionsOn(LocalDate date) {
        int week = weekNumber(date);
        if (!isVisibleWeek(week)) return Collections.emptyList();
        List<CourseSession> result = new ArrayList<>();
        for (ScheduleRule rule : importedRules) if (rule.appliesTo(date, week)) result.add(rule.toSession(date));
        Collections.sort(result);
        return result;
    }
    public static List<CourseSession> sessionsInWeek(int week) {
        if (!isVisibleWeek(week)) return Collections.emptyList();
        List<CourseSession> result = new ArrayList<>();
        LocalDate monday = mondayOfWeek(week);
        // Scan rules once rather than repeating all filters and sorts for seven days.
        for(ScheduleRule rule:importedRules){
            LocalDate date=monday.plusDays(rule.getDayOfWeek().getValue()-1L);
            if(rule.appliesTo(date,week))result.add(rule.toSession(date));
        }
        Collections.sort(result);
        return result;
    }
    public static int totalSessionCount() {
        int total = 0;
        for (int week = firstVisibleWeek(); week <= lastVisibleWeek(); week++) total += sessionsInWeek(week).size();
        return total;
    }
    public static synchronized void setImportedRules(List<ScheduleRule> rules) {
        setImportedRules(rules, today());
    }
    public static synchronized void setImportedRules(List<ScheduleRule> rules, LocalDate startDate) {
        importedStartDate = startDate;
        if (rules == null || rules.isEmpty()) {
            importedRules = Collections.emptyList();
        } else {
            List<ScheduleRule> normalized=new ArrayList<>();
            for(ScheduleRule rule:rules)normalized.add(SchoolPeriodTimes.normalize(rule));
            importedRules = Collections.unmodifiableList(normalized);
        }
    }
    public static synchronized void setSchoolStartDate(LocalDate startDate) { importedStartDate = startDate; }
    public static LocalDate schoolStartDate() { return importedStartDate; }
    public static LocalDate weekOneMonday() { return weekMonday(importedStartDate); }
    public static int firstVisibleWeek() { return 1; }
    public static int lastVisibleWeek() {
        int last = 0;
        for (ScheduleRule rule : importedRules) last = Math.max(last, rule.getToWeek());
        return last;
    }
    public static boolean isVisibleWeek(int week) {
        return hasImportedRules() && week >= firstVisibleWeek() && week <= lastVisibleWeek();
    }
    public static String rangeText() {
        return hasImportedRules() ? "第1—" + lastVisibleWeek() + "周" : "未设置课表";
    }
    public static boolean hasImportedRules() { return !importedRules.isEmpty(); }
    public static int importedRuleCount() { return importedRules.size(); }
    public static List<ScheduleRule> rules() { return new ArrayList<>(importedRules); }
}
