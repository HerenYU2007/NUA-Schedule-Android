import cn.nua.wenguan.schedule.ui.*;
import cn.nua.wenguan.schedule.data.*;
import cn.nua.wenguan.schedule.model.*;
import java.time.*;
import java.util.*;

public final class ValidateConflictGrid {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static AgendaEvent event(String id,LocalDate date,int start,int end,boolean allDay,boolean done){return new AgendaEvent(id,id,"地点",date,LocalTime.of(start/60,start%60),LocalTime.of(end/60,end%60),allDay,done,false);}
    private static CourseSession course(LocalDate date,String start,String end,String periods,String name){return new ScheduleRule(1,16,date.getDayOfWeek(),start,end,periods,name,"文313","课程","老师").toSession(date);}
    public static void main(String[] arguments){
        LocalDate date=LocalDate.of(2026,10,15);
        CourseSession morning=course(date,"08:55","11:40","第2—4节","课程A"),afternoon=course(date,"14:00","15:40","第5—6节","课程B");
        List<CourseSession> courses=Arrays.asList(morning,afternoon);
        AgendaEvent longEvent=event("长日程",date,9*60,17*60,false,false),shortEvent=event("短日程",date,10*60,11*60,false,false),otherDate=event("另一天",date.plusDays(1),9*60,17*60,false,false);
        Map<String,AgendaConflictIndex.Report> reports=AgendaConflictIndex.build(Arrays.asList(longEvent,shortEvent,otherDate),d->courses);
        check(reports.size()==2&&!reports.containsKey(otherDate.id),"date filtering and two affected agendas");
        check(reports.get(longEvent.id).description().contains("课程A")&&reports.get(longEvent.id).description().contains("课程B")&&reports.get(longEvent.id).description().contains("短日程"),"specific courses and event names");
        AgendaGridPolicy.Result display=AgendaGridPolicy.plan(courses,Arrays.asList(longEvent,shortEvent,otherDate));
        check(display.separateEvents.equals(new HashSet<>(Arrays.asList(longEvent.id,shortEvent.id))),"long overlap chain cannot narrow course columns");
        check(display.markedCourses.contains(morning.getRuleId())&&display.markedCourses.contains(afternoon.getRuleId()),"affected course markers");
        List<AgendaEvent> adjacency=Arrays.asList(event("A",date,480,540,false,false),event("B",date,540,600,false,false),event("全天",date,480,600,true,false),event("已完成",date,490,520,false,true));
        check(AgendaConflictIndex.build(adjacency,d->Collections.emptyList()).isEmpty(),"adjacent/all-day/completed do not generate time warnings");
        GridOccupancyPolicy occupied=new GridOccupancyPolicy();occupied.reserve(3,2,4);
        for(int day=0;day<7;day++)for(int period=1;period<=12;period++)check(occupied.free(day,period,period)==!(day==3&&period>=2&&period<=4),"whole occupied cell including visual padding blocked");
        check(!occupied.free(3,1,5)&&occupied.free(3,5,7),"draft resize/move range occupancy");
        for(int[] invalid:new int[][]{{-1,1,1},{7,1,1},{0,0,1},{0,1,13},{0,5,4}})check(!occupied.free(invalid[0],invalid[1],invalid[2]),"invalid cells not selectable");
        GridOccupancyPolicy breaks=new GridOccupancyPolicy();breaks.reserveTime(0,LocalTime.of(8,45),LocalTime.of(8,55));
        check(breaks.free(0,1,2),"exact break does not occupy adjacent lessons");
        breaks.reserveTime(0,LocalTime.of(8,44),LocalTime.of(8,56));check(!breaks.free(0,1,1)&&!breaks.free(0,2,2),"partial time overlap reserves both slots");
        Random random=new Random(263601064L);
        for(int fixture=0;fixture<250;fixture++){
            List<AgendaEvent> events=new ArrayList<>();for(int n=0;n<40;n++){int start=420+random.nextInt(750),end=Math.min(1320,start+1+random.nextInt(180));events.add(event("e"+n,date.plusDays(random.nextInt(3)),start,end,random.nextInt(12)==0,random.nextInt(8)==0));}
            Set<String> expected=new HashSet<>();
            for(AgendaEvent a:events)if(!a.done&&!a.allDay){
                for(AgendaEvent b:events)if(!a.id.equals(b.id)&&!b.done&&!b.allDay&&a.date.equals(b.date)&&a.start.isBefore(b.end)&&b.start.isBefore(a.end))expected.add(a.id);
                for(CourseSession c:courses)if(a.date.equals(c.getDate())&&a.start.isBefore(c.getEndTime())&&c.getStartTime().isBefore(a.end))expected.add(a.id);
            }
            Map<String,AgendaConflictIndex.Report> actual=AgendaConflictIndex.build(events,d->courses);check(actual.keySet().equals(expected),"sweep matches brute-force warning membership");
            AgendaGridPolicy.Result layout=AgendaGridPolicy.plan(courses,events);
            for(AgendaEvent a:events){
                float[] x=a.allDay?null:GridPlacementPolicy.agendaSpan(a.start,a.end);boolean overlap=false;
                if(x!=null){
                    for(AgendaEvent b:events)if(!a.id.equals(b.id)&&a.date.equals(b.date)&&!b.allDay){float[] y=GridPlacementPolicy.agendaSpan(b.start,b.end);if(y!=null&&x[0]<y[1]-.0001f&&y[0]<x[1]-.0001f)overlap=true;}
                    for(CourseSession c:courses)if(a.date.equals(c.getDate())){int[] y=PeriodGrid.span(c);if(y!=null&&x[0]<y[1]-.0001f&&y[0]-1<x[1]-.0001f)overlap=true;}
                }
                check(layout.separateEvents.contains(a.id)==overlap,"visual overlap goes to readable list, including completed entries");
            }
        }
        List<AgendaEvent> stress=new ArrayList<>();for(int n=0;n<2000;n++)stress.add(event("stress"+n,date,540,600,false,false));
        Map<String,AgendaConflictIndex.Report> many=AgendaConflictIndex.build(stress,d->Collections.emptyList());check(many.size()==2000,"all events detected even when detail list capped");
        for(AgendaConflictIndex.Report report:many.values())check(report.description().split("\n").length<=11,"bounded readable report");
        System.out.println("Conflict/grid: "+checks+" checks passed. Native visual/touch results require device verification.");
    }
}
