package cn.nua.wenguan.schedule.data;
import cn.nua.wenguan.schedule.model.*;
import java.util.*;

/** Half-open clock intervals: adjacent lessons are not conflicts. No Android dependencies. */
public final class ScheduleConflicts {
    private ScheduleConflicts() { }
    public static boolean overlap(ScheduleRule a, ScheduleRule b) {
        return a.getDayOfWeek() == b.getDayOfWeek() && a.getFromWeek() <= b.getToWeek() && b.getFromWeek() <= a.getToWeek()
                && a.getStartTime().isBefore(b.getEndTime()) && b.getStartTime().isBefore(a.getEndTime());
    }
    public static List<String> courses(List<ScheduleRule> rules) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (int a = 0; a < rules.size(); a++) for (int b = a + 1; b < rules.size(); b++) if (overlap(rules.get(a), rules.get(b))) {
            result.add(rules.get(a).getCourse() + " / " + rules.get(b).getCourse() + " · 周" + "一二三四五六日".charAt(rules.get(a).getDayOfWeek().getValue()-1));
            if (result.size() >= 10) return new ArrayList<>(result);
        }
        return new ArrayList<>(result);
    }
    public static List<String> agenda(AgendaEvent event, List<AgendaEvent> events, List<CourseSession> lessons) {
        List<String> result = new ArrayList<>();
        if (event.allDay || event.done) return result;
        for (CourseSession s : lessons) if (s.getDate().equals(event.date) && event.start.isBefore(s.getEndTime()) && s.getStartTime().isBefore(event.end)) result.add(s.getCourse());
        for (AgendaEvent e : events) if (!e.id.equals(event.id) && e.date.equals(event.date) && !e.done && !e.allDay && event.start.isBefore(e.end) && e.start.isBefore(event.end)) result.add(e.title);
        return result;
    }
}
