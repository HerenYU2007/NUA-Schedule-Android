package cn.nua.wenguan.schedule.data;

import cn.nua.wenguan.schedule.model.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;

/** Sweep each date once; cap display details, not the number of conflicting events detected. */
public final class AgendaConflictIndex {
    private AgendaConflictIndex() { }
    public static final class Report {
        public final AgendaEvent event;
        private final LinkedHashSet<String> details=new LinkedHashSet<>();
        private Report(AgendaEvent event){this.event=event;}
        public String description(){return String.join("\n",details)+(details.size()>=10?"\n仅列出前10项重叠安排":"");}
        private void add(String kind,String name,LocalTime start,LocalTime end){if(details.size()<10)details.add(kind+"："+name+" · "+start+"—"+end);}
    }
    private static Report report(Map<String,Report> reports,AgendaEvent event){return reports.computeIfAbsent(event.id,ignored->new Report(event));}
    public static Map<String,Report> build(List<AgendaEvent> events,Function<LocalDate,List<CourseSession>> lessons){
        Map<LocalDate,List<AgendaEvent>> byDate=new TreeMap<>();
        for(AgendaEvent event:events)if(!event.allDay&&!event.done)byDate.computeIfAbsent(event.date,ignored->new ArrayList<>()).add(event);
        Map<String,Report> result=new LinkedHashMap<>();
        for(Map.Entry<LocalDate,List<AgendaEvent>> entry:byDate.entrySet()){
            List<CourseSession> courses=lessons.apply(entry.getKey());List<AgendaEvent> sorted=entry.getValue();sorted.sort(Comparator.comparing((AgendaEvent event)->event.start).thenComparing(event->event.id));
            PriorityQueue<AgendaEvent> endings=new PriorityQueue<>(Comparator.comparing((AgendaEvent event)->event.end).thenComparing(event->event.id));
            LinkedHashMap<String,AgendaEvent> active=new LinkedHashMap<>();
            for(AgendaEvent event:sorted){
                while(!endings.isEmpty()&&!endings.peek().end.isAfter(event.start)){AgendaEvent expired=endings.remove();active.remove(expired.id);}
                int shown=0;
                for(AgendaEvent other:active.values()){
                    LocalTime start=event.start.isAfter(other.start)?event.start:other.start,end=event.end.isBefore(other.end)?event.end:other.end;
                    report(result,event).add("日程",other.title,start,end);report(result,other).add("日程",event.title,start,end);
                    if(++shown>=10)break;
                }
                for(CourseSession course:courses)if(course.getDate().equals(event.date)&&event.start.isBefore(course.getEndTime())&&course.getStartTime().isBefore(event.end)){
                    LocalTime start=event.start.isAfter(course.getStartTime())?event.start:course.getStartTime(),end=event.end.isBefore(course.getEndTime())?event.end:course.getEndTime();
                    report(result,event).add("课程",course.getCourse(),start,end);
                }
                endings.add(event);active.put(event.id,event);
            }
        }
        List<Report> ordered=new ArrayList<>(result.values());ordered.sort(Comparator.comparing((Report r)->r.event.date).thenComparing(r->r.event.start).thenComparing(r->r.event.id));
        result.clear();for(Report r:ordered)result.put(r.event.id,r);return result;
    }
}
