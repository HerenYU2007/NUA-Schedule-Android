import cn.nua.wenguan.schedule.data.*;
import cn.nua.wenguan.schedule.model.*;
import cn.nua.wenguan.schedule.widget.WeekBitmapPolicy;
import java.time.*;
import java.util.*;
public final class ValidateLocal123 {
    private static int count;
    private static void check(boolean valid,String message){count++;if(!valid)throw new AssertionError(message);}
    private static ScheduleRule rule(String id,int day,String start,String end,String periods,String name){
        return new ScheduleRule(1,16,DayOfWeek.of(day),start,end,periods,name,"文313","教务同步","教师",id,0xff99ccaa,"2.5","原备注");
    }
    private static void rejected(Runnable run){try{run.run();throw new AssertionError("unsafe merge accepted");}catch(IllegalArgumentException expected){count++;}}
    public static void main(String[] args){
        ScheduleRule clicked=rule("a",2,"08:50","11:40","第2—4节","同名课");
        ScheduleRule other=rule("b",5,"14:00","15:40","第5—6节","同名课");
        ScheduleRule renamed=rule("a",2,"08:55","11:40","第2—4节","更名课");
        List<ScheduleRule> current=Arrays.asList(clicked,other);
        List<ScheduleRule> merged=SlotEditPolicy.merge(current,Collections.singleton("a"),Collections.singletonList(renamed));
        check(merged.size()==2&&merged.contains(other)&&merged.contains(renamed),"only clicked ID replaced");
        check(current.get(0)==clicked&&current.get(1)==other,"input list unchanged");
        List<ScheduleRule> removed=SlotEditPolicy.merge(current,Collections.singleton("a"),Collections.emptyList());
        check(removed.size()==1&&removed.get(0)==other,"deletion never deletes another same-name slot");
        rejected(()->SlotEditPolicy.merge(current,Collections.singleton("missing"),Collections.emptyList()));
        rejected(()->SlotEditPolicy.merge(current,Collections.singleton("a"),Collections.singletonList(other)));
        ScheduleRule normalized=SchoolPeriodTimes.normalize(clicked);
        check(normalized.getStartTime().equals(LocalTime.of(8,55))&&normalized.getEndTime().equals(LocalTime.of(11,40)),"old 2-4 migrated");
        check(normalized.getId().equals(clicked.getId())&&normalized.getNote().equals("原备注")&&normalized.getColor()==clicked.getColor(),"migration keeps ID and metadata");
        check(SchoolPeriodTimes.normalize(normalized)==normalized,"migration idempotent");
        String[] starts={"08:00","08:55","10:00","10:55"},ends={"08:45","09:40","10:45","11:40"};
        String[] oldStarts={"08:00","08:50","10:00","10:50"},oldEnds={"08:50","09:40","10:50","11:40"};
        for(int n=1;n<=4;n++){
            check(SchoolPeriodTimes.start(n).equals(starts[n-1])&&SchoolPeriodTimes.end(n).equals(ends[n-1]),"requested morning clock");
            ScheduleRule single=SchoolPeriodTimes.normalize(rule("single"+n,1,oldStarts[n-1],oldEnds[n-1],"第"+n+"节","上午"));
            check(single.getStartTime().toString().equals(starts[n-1])&&single.getEndTime().toString().equals(ends[n-1]),"single morning migration");
        }
        for(int n=5;n<=12;n++){
            ScheduleRule afternoon=rule("after"+n,1,SchoolPeriodTimes.start(n),SchoolPeriodTimes.end(n),"第"+n+"节","非上午");
            check(SchoolPeriodTimes.normalize(afternoon)==afternoon,"afternoon/night never altered");
        }
        ScheduleRule custom=rule("custom",2,"08:50","11:40","自定义时间","自定义");
        check(SchoolPeriodTimes.normalize(custom)==custom,"explicit custom time never migrated");
        ScheduleRule unusual=rule("unusual",2,"09:00","11:40","第2—4节","特殊时间");
        check(SchoolPeriodTimes.normalize(unusual)==unusual,"non-standard literal clock kept");
        ScheduleRepository.setImportedRules(current,LocalDate.of(2026,9,7));
        for(int week=0;week<=18;week++){
            List<CourseSession> expected=new ArrayList<>();LocalDate monday=ScheduleRepository.mondayOfWeek(week);
            for(int day=0;day<7;day++)expected.addAll(ScheduleRepository.sessionsOn(monday.plusDays(day)));
            Collections.sort(expected);List<CourseSession> actual=ScheduleRepository.sessionsInWeek(week);
            check(actual.size()==expected.size(),"single-scan week size");
            for(int i=0;i<actual.size();i++)check(actual.get(i).getRuleId().equals(expected.get(i).getRuleId())&&actual.get(i).getTimeRange().equals(expected.get(i).getTimeRange()),"single-scan week identity/time/order");
        }
        long total=WeekBitmapPolicy.totalBudget(1080,2400);
        int[] large=WeekBitmapPolicy.pixels(344,410,3,total/2);
        check(large[0]>512&&large[1]>850,"normal phone widget sharper than previous fixed caps");
        for(int variants=1;variants<=4;variants++)for(int width:new int[]{1,234,344,800})for(int height:new int[]{1,230,410,1000})for(float density:new float[]{Float.NaN,-1,1,2.75f,4,Float.MAX_VALUE}){
            int[] size=WeekBitmapPolicy.pixels(width,height,density,total/variants);
            check(size[0]>=1&&size[0]<=2048&&size[1]>=1&&size[1]<=3072,"pixel dimension bounds");
            check((long)size[0]*size[1]*4*variants<=total,"combined ARGB images fit total budget");
        }
        System.out.println("Local 1.2.3: "+count+" checks passed (slot isolation, morning-only clock/migration, week scan equivalence, high-density widget memory). No device FPS claim.");
    }
}
