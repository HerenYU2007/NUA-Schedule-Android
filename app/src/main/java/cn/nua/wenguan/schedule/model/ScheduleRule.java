package cn.nua.wenguan.schedule.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public final class ScheduleRule {
    private final int fromWeek;
    private final int toWeek;
    private final DayOfWeek dayOfWeek;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String periods;
    private final String course;
    private final String room;
    private final String type;
    private final String teacher;

    public ScheduleRule(
            int fromWeek,
            int toWeek,
            DayOfWeek dayOfWeek,
            String startTime,
            String endTime,
            String periods,
            String course,
            String room,
            String type,
            String teacher
    ) {
        this.fromWeek = fromWeek;
        this.toWeek = toWeek;
        this.dayOfWeek = dayOfWeek;
        this.startTime = LocalTime.parse(startTime);
        this.endTime = LocalTime.parse(endTime);
        this.periods = periods;
        this.course = course;
        this.room = room;
        this.type = type;
        this.teacher = teacher;
    }

    public boolean appliesTo(LocalDate date, int week) {
        return week >= fromWeek && week <= toWeek && date.getDayOfWeek() == dayOfWeek;
    }

    public CourseSession toSession(LocalDate date) {
        return new CourseSession(date, startTime, endTime, periods, course, room, type, teacher);
    }

    public int getFromWeek() { return fromWeek; }
    public int getToWeek() { return toWeek; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getPeriods() { return periods; }
    public String getCourse() { return course; }
    public String getRoom() { return room; }
    public String getType() { return type; }
    public String getTeacher() { return teacher; }
}
