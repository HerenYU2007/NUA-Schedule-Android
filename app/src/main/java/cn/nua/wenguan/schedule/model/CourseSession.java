package cn.nua.wenguan.schedule.model;

import java.time.LocalDate;
import java.time.LocalTime;

public final class CourseSession implements Comparable<CourseSession> {
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String periods;
    private final String course;
    private final String room;
    private final String type;
    private final String teacher;

    public CourseSession(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String periods,
            String course,
            String room,
            String type,
            String teacher
    ) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.periods = periods;
        this.course = course;
        this.room = room;
        this.type = type;
        this.teacher = teacher;
    }

    public LocalDate getDate() { return date; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getPeriods() { return periods; }
    public String getCourse() { return course; }
    public String getRoom() { return room; }
    public String getType() { return type; }
    public String getTeacher() { return teacher; }

    public String getTimeRange() {
        return String.format("%02d:%02d–%02d:%02d",
                startTime.getHour(), startTime.getMinute(), endTime.getHour(), endTime.getMinute());
    }

    @Override
    public int compareTo(CourseSession other) {
        int byDate = date.compareTo(other.date);
        return byDate != 0 ? byDate : startTime.compareTo(other.startTime);
    }
}
