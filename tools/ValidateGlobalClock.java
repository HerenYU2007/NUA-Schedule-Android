import cn.nua.wenguan.schedule.data.*;
import cn.nua.wenguan.schedule.model.*;
import java.time.*;
import java.util.*;
public final class ValidateGlobalClock {
    private static int count;
    private static void check(boolean ok){count++;if(!ok)throw new AssertionError("clock case "+count);}
    private static ScheduleRule rule(String start,String end,String periods){return new ScheduleRule(1,16,DayOfWeek.MONDAY,start,end,periods,"测试课","文313","理论","教师","clock-test",0xff99ccaa,"2","备注");}
    private static void reject(String[] s,String[] e){String before=SchoolPeriodTimes.revision();try{SchoolPeriodTimes.configure(s,e);throw new AssertionError("invalid clock accepted");}catch(IllegalArgumentException expected){check(before.equals(SchoolPeriodTimes.revision()));}}
    public static void main(String[] args){
        check(SchoolPeriodTimes.end(5).equals("14:45"));check(SchoolPeriodTimes.start(6).equals("14:55"));check(SchoolPeriodTimes.end(7).equals("16:45"));
        String[] lateStarts={"16:55","18:30","19:25","20:30","21:25"},lateEnds={"17:40","19:15","20:10","21:15","22:00"};
        for(int p=8;p<=12;p++){check(SchoolPeriodTimes.start(p).equals(lateStarts[p-8]));check(SchoolPeriodTimes.end(p).equals(lateEnds[p-8]));}
        String[] previousStarts=SchoolPeriodTimes.defaultStarts(),previousEnds=SchoolPeriodTimes.defaultEnds();
        previousStarts[8]="16:50";previousStarts[10]="19:20";previousStarts[12]="21:15";previousEnds[9]="19:20";
        String[][] upgraded=SchoolPeriodTimes.upgradePreviousDefault(previousStarts,previousEnds);
        check(upgraded[0][8].equals("16:55")&&upgraded[1][9].equals("19:15"));
        check(previousStarts[8].equals("16:50"));
        previousStarts[1]="07:50";previousEnds[1]="08:35";
        String[][] customGlobal=SchoolPeriodTimes.upgradePreviousDefault(previousStarts,previousEnds);
        check(Arrays.equals(customGlobal[0],previousStarts)&&Arrays.equals(customGlobal[1],previousEnds));
        String[] oldStarts={"16:50","18:30","19:20","20:30","21:15"},oldEnds={"17:40","19:20","20:10","21:15","22:00"};
        for(int p=8;p<=12;p++){
            ScheduleRule migrated=SchoolPeriodTimes.normalize(rule(oldStarts[p-8],oldEnds[p-8],"第"+p+"节"));
            check(migrated.getStartTime().toString().equals(lateStarts[p-8])&&migrated.getEndTime().toString().equals(lateEnds[p-8]));
            ScheduleRule customLate=rule(oldStarts[p-8],oldEnds[p-8],"自定义时间");check(SchoolPeriodTimes.normalize(customLate)==customLate);
        }
        ScheduleRule lateRange=SchoolPeriodTimes.normalize(rule("16:50","22:00","第8—12节"));check(lateRange.getStartTime().toString().equals("16:55"));
        ScheduleRule[] legacy={rule("14:00","14:50","第5节"),rule("14:50","15:40","第6节"),rule("16:00","16:50","第7节"),rule("14:00","16:50","第5—7节")};
        for(ScheduleRule old:legacy){ScheduleRule next=SchoolPeriodTimes.normalize(old);check(next.followsPeriodClock());check(next.getId().equals(old.getId())&&next.getNote().equals(old.getNote())&&next.getColor()==old.getColor());check(SchoolPeriodTimes.normalize(next)==next);}
        check(SchoolPeriodTimes.normalize(legacy[1]).getStartTime().equals(LocalTime.of(14,55)));
        check(SchoolPeriodTimes.normalize(legacy[3]).getEndTime().equals(LocalTime.of(16,45)));
        ScheduleRule custom=rule("14:00","14:50","自定义时间"),unusual=rule("14:03","14:39","第5节");
        check(SchoolPeriodTimes.normalize(custom)==custom);check(SchoolPeriodTimes.normalize(unusual)==unusual);
        String[] starts=SchoolPeriodTimes.starts(),ends=SchoolPeriodTimes.ends();starts[1]="07:50";ends[1]="08:35";SchoolPeriodTimes.configure(starts,ends);
        ScheduleRule linked=SchoolPeriodTimes.normalize(rule("08:00","08:45","第1节"));check(linked.getStartTime().toString().equals("07:50"));
        starts[1]="07:40";ends[1]="08:25";SchoolPeriodTimes.configure(starts,ends);
        check(SchoolPeriodTimes.normalize(linked).getStartTime().toString().equals("07:40"));check(SchoolPeriodTimes.startTime(1).equals(LocalTime.of(7,40)));
        starts[1]="00:00";check(SchoolPeriodTimes.start(1).equals("07:40"));
        String[] copy=SchoolPeriodTimes.starts();copy[1]="01:00";check(SchoolPeriodTimes.start(1).equals("07:40"));
        starts=SchoolPeriodTimes.starts();ends=SchoolPeriodTimes.ends();ends[1]=starts[1];reject(starts,ends);
        ends[1]="09:30";reject(starts,ends);ends[1]="08:25";starts[1]="24:00";reject(starts,ends);
        starts[1]="7:40";reject(starts,ends);reject(new String[2],new String[2]);
        SchoolPeriodTimes.configure(SchoolPeriodTimes.defaultStarts(),SchoolPeriodTimes.defaultEnds());
        check(SchoolPeriodTimes.normalize(linked).getStartTime().toString().equals("08:00"));
        ScheduleRepository.setImportedRules(Arrays.asList(legacy),LocalDate.of(2026,9,7));
        check(ScheduleRepository.sessionsOn(LocalDate.of(2026,9,7)).stream().anyMatch(s->s.getEndTime().equals(LocalTime.of(16,45))));
        System.out.println("Global clock: "+count+" checks passed (migration, metadata, custom protection, repeated edits, validation and reset). Not device storage/UI testing.");
    }
}
