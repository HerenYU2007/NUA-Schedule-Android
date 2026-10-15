package cn.nua.wenguan.schedule.ui;

import cn.nua.wenguan.schedule.model.*;
import java.time.LocalDate;
import java.util.*;

/** Never squeeze a whole collision chain into unreadable lanes. Agenda overlaps get a readable list. */
public final class AgendaGridPolicy {
    private AgendaGridPolicy() { }
    public static final class Result {
        public final Set<String> separateEvents=new HashSet<>(),markedCourses=new HashSet<>();
    }
    private static final class Item {
        final float top,bottom;final String id;final boolean agenda;
        Item(float top,float bottom,String id,boolean agenda){this.top=top;this.bottom=bottom;this.id=id;this.agenda=agenda;}
    }
    public static Result plan(List<CourseSession> courses,List<AgendaEvent> events){
        Map<LocalDate,List<Item>> dates=new HashMap<>();Result result=new Result();
        for(CourseSession course:courses){int[] span=PeriodGrid.span(course);if(span!=null)dates.computeIfAbsent(course.getDate(),ignored->new ArrayList<>()).add(new Item(span[0]-1,span[1],course.getRuleId(),false));}
        for(AgendaEvent event:events){float[] span=event.allDay?null:GridPlacementPolicy.agendaSpan(event.start,event.end);if(span!=null)dates.computeIfAbsent(event.date,ignored->new ArrayList<>()).add(new Item(span[0],span[1],event.id,true));}
        for(List<Item> items:dates.values()){
            items.sort(Comparator.comparingDouble((Item item)->item.top).thenComparingDouble(item->item.bottom));
            for(int first=0;first<items.size();){
                int last=first+1;float edge=items.get(first).bottom;boolean hasAgenda=items.get(first).agenda;
                while(last<items.size()&&items.get(last).top<edge-.0001f){hasAgenda|=items.get(last).agenda;edge=Math.max(edge,items.get(last).bottom);last++;}
                if(hasAgenda&&last-first>1)for(int n=first;n<last;n++){Item item=items.get(n);(item.agenda?result.separateEvents:result.markedCourses).add(item.id);}
                first=last;
            }
        }
        for(CourseSession course:courses)for(AgendaEvent event:events)if(!event.allDay&&event.date.equals(course.getDate())&&event.start.isBefore(course.getEndTime())&&course.getStartTime().isBefore(event.end))result.markedCourses.add(course.getRuleId());
        return result;
    }
}
