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
    private final String id, credits, note;
    private final int color;

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
        this(fromWeek, toWeek, dayOfWeek, startTime, endTime, periods, course, room, type, teacher,
                java.util.UUID.nameUUIDFromBytes((fromWeek+"|"+toWeek+"|"+dayOfWeek+"|"+startTime+"|"+endTime+"|"+course+"|"+room+"|"+teacher).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString(), 0, "", "");
    }

    public ScheduleRule(int fromWeek, int toWeek, DayOfWeek dayOfWeek, String startTime, String endTime,
            String periods, String course, String room, String type, String teacher, String id, int color, String credits, String note) {
        if (fromWeek < 1 || toWeek > 53 || fromWeek > toWeek || dayOfWeek == null || course == null || course.trim().isEmpty()
                || course.length() > 100 || room == null || room.length() > 200 || teacher == null || teacher.length() > 100
                || periods == null || type == null || id == null || !id.matches("[A-Za-z0-9_-]{1,100}")
                || credits == null || credits.length() > 20 || note == null || note.length() > 1000)
            throw new IllegalArgumentException("课程信息不完整，请检查名称、周数和时段");
        this.fromWeek = fromWeek;
        this.toWeek = toWeek;
        this.dayOfWeek = dayOfWeek;
        this.startTime = LocalTime.parse(startTime);
        this.endTime = LocalTime.parse(endTime);
        this.periods = periods;
        this.course = course.trim();
        this.room = room;
        this.type = type;
        this.teacher = teacher;
        if (!this.endTime.isAfter(this.startTime)) throw new IllegalArgumentException("结束时间必须晚于开始时间");
        this.id = id; this.color = color == 0 ? 0 : color | 0xff000000; this.credits = credits; this.note = note;
    }

    public boolean appliesTo(LocalDate date, int week) {
        return week >= fromWeek && week <= toWeek && date.getDayOfWeek() == dayOfWeek;
    }

    public CourseSession toSession(LocalDate date) {
        return new CourseSession(date, startTime, endTime, periods, course, room, type, teacher, id, color, credits, note);
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
    public String getId() { return id; }
    public int getColor() { return color; }
    public String getCredits() { return credits; }
    public String getNote() { return note; }
}
