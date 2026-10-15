import cn.nua.wenguan.schedule.ReminderPolicy;
import cn.nua.wenguan.schedule.data.*;
import cn.nua.wenguan.schedule.model.*;
import cn.nua.wenguan.schedule.ui.*;
import java.time.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import jxl.Workbook;
public final class ValidateFeatureBatch {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static ScheduleRule rule(int from,int to,int day,String start,String end,String name){return new ScheduleRule(from,to,DayOfWeek.of(day),start,end,"第1—2节",name,"文313","手动编辑","老师");}
    public static void main(String[] args)throws Exception{
        ScheduleRule a=rule(1,16,2,"08:00","09:40","管理学原理"),b=rule(6,8,2,"09:00","11:40","社会调查方法");
        check(ScheduleConflicts.overlap(a,b),"actual overlap");check(!ScheduleConflicts.overlap(a,rule(1,16,2,"09:40","11:40","相邻课")),"adjacent clocks");check(!ScheduleConflicts.overlap(a,rule(17,18,2,"08:00","09:40","不同周")),"disjoint weeks");check(!ScheduleConflicts.overlap(a,rule(1,16,3,"08:00","09:40","不同日")),"disjoint day");
        check(ScheduleConflicts.courses(Arrays.asList(a,b)).size()==1,"conflict warning");
        check(a.getId().equals(rule(1,16,2,"08:00","09:40","管理学原理").getId()),"legacy IDs deterministic");
        ScheduleRule custom=new ScheduleRule(2,4,DayOfWeek.FRIDAY,"12:10","13:40","自定义时间","自定义课","教室","手动编辑","老师","fixed-id",0x123456,"2.5","备注😀");CourseSession s=custom.toSession(LocalDate.of(2026,10,16));check(s.getRuleId().equals("fixed-id")&&s.getColor()==0xff123456&&s.getNote().equals("备注😀")&&s.getCredits().equals("2.5"),"metadata reaches sessions");check(PeriodGrid.span(s)==null,"custom times never mapped to incorrect standard periods");
        for(Runnable invalid:new Runnable[]{()->rule(0,2,1,"08:00","09:40","非法"),()->rule(1,54,1,"08:00","09:40","非法"),()->rule(1,2,1,"09:40","08:00","非法"),()->rule(1,2,1,"08:00","09:40","")}){try{invalid.run();throw new AssertionError("accepted invalid rule");}catch(IllegalArgumentException good){checks++;}}
        for(int lesson=-1;lesson<=2;lesson++)for(int course=-1;course<=2;course++){int expected=lesson>=0?lesson:course>=0?course:1;check(ReminderPolicy.effectiveMode(true,lesson,course,false,false,1)==expected,"override precedence");check(ReminderPolicy.effectiveMode(false,lesson,course,false,true,2)==0,"master off");}
        check(ReminderPolicy.effectiveMode(true,-1,-1,true,false,1)==0,"selected scope");check(ReminderPolicy.effectiveMode(true,-1,-1,true,true,2)==2,"selected alarm");check(ReminderPolicy.eligible(1000,5000,1500,1,false),"upcoming reminder");check(!ReminderPolicy.eligible(5000,5000,1500,1,false),"ended class");check(!ReminderPolicy.eligible(200000,500000,0,2,false),"stale alarms do not ring");check(ReminderPolicy.eligible(200000,500000,0,1,false),"briefly late notifications retained");check(!ReminderPolicy.eligible(1000,5000,1500,1,true),"already fired");
        check(WeekSwipePolicy.directionWithVelocity(-30,1,-1000,1)==1,"short quick fling");check(WeekSwipePolicy.directionWithVelocity(-30,1,-100,1)==0,"small slow drag");check(WeekSwipePolicy.directionWithVelocity(-100,120,-1000,1)==0,"vertical scroll");check(WeekSwipePolicy.directionWithVelocity(-100,1,Float.NaN,1)==0,"invalid speed");
        LocalDate day=LocalDate.of(2026,9,8);AgendaEvent event=new AgendaEvent("e","事项","地点",day,LocalTime.of(9,0),LocalTime.of(10,0),false,false,false);check(ScheduleConflicts.agenda(event,Collections.emptyList(),Arrays.asList(a.toSession(day))).size()==1,"agenda versus class");check(ScheduleConflicts.agenda(new AgendaEvent("e","全天","",day,null,null,true,false,false),Collections.emptyList(),Arrays.asList(a.toSession(day))).isEmpty(),"all-day not constant conflict");
        String name="测试😀课程".repeat(25)+",分号;反斜杠\\";ScheduleRule unicode=rule(1,1,2,"08:00","09:40",name.substring(0,Math.min(95,name.length())));String ics=ScheduleExport.calendar(Arrays.asList(unicode),LocalDate.of(2026,9,9));check(ics.startsWith("BEGIN:VCALENDAR\r\n")&&ics.endsWith("END:VCALENDAR\r\n"),"ICS envelope");check(ics.contains("DTSTART:20260908T000000Z"),"Beijing to UTC and Monday mapping");for(String line:ics.split("\r\n"))check(line.getBytes(StandardCharsets.UTF_8).length<=75,"UTF-8 calendar folding");check(!ics.contains("VALARM"),"export never creates surprise alarms");
        ByteArrayOutputStream output=new ByteArrayOutputStream();ScheduleExport.xls(output,Arrays.asList(a,custom,rule(1,1,1,"08:00","09:40","=SUM(A1:A2)")),LocalDate.of(2026,9,9));try{Workbook book=Workbook.getWorkbook(new ByteArrayInputStream(output.toByteArray()));check(book.getSheet(0).getRows()==4,"XLS rows");check(book.getSheet(0).getCell(0,3).getContents().equals("=SUM(A1:A2)")&&book.getSheet(0).getCell(0,3).getType()==jxl.CellType.LABEL,"formula-like course remains literal");check(book.getSheet(1).getCell(0,0).getContents().contains("2026-09-09"),"XLS start date");book.close();}finally{output.close();}
        System.out.println("Feature batch: "+checks+" checks passed (metadata, conflict boundaries, reminder scopes/late/fired, swipe intent, ICS Unicode/date folding, XLS round-trip). JSON/device behavior needs Android runtime.");
    }
}
