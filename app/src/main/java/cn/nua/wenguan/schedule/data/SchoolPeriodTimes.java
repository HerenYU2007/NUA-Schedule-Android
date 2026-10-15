package cn.nua.wenguan.schedule.data;
import cn.nua.wenguan.schedule.model.ScheduleRule;
import java.util.regex.*;

/** School clock. Only periods 1-4 changed in 1.2.3; custom times remain literal. */
public final class SchoolPeriodTimes {
    private static final String[] START={"","08:00","08:55","10:00","10:55","14:00","14:50","16:00","16:50","18:30","19:20","20:30","21:15"};
    private static final String[] END={"","08:45","09:40","10:45","11:40","14:50","15:40","16:50","17:40","19:20","20:10","21:15","22:00"};
    private static final Pattern LABEL=Pattern.compile("第?\\s*(\\d{1,2})(?:\\s*[-—–~至]\\s*(\\d{1,2}))?\\s*节");
    private static final java.time.LocalTime[] START_TIME=new java.time.LocalTime[13],END_TIME=new java.time.LocalTime[13];
    static{for(int period=1;period<=12;period++){START_TIME[period]=java.time.LocalTime.parse(START[period]);END_TIME[period]=java.time.LocalTime.parse(END[period]);}}
    public static java.time.LocalTime startTime(int period){return START_TIME[period];}
    public static java.time.LocalTime endTime(int period){return END_TIME[period];}
    private SchoolPeriodTimes() { }
    public static String start(int period){return START[period];}
    public static String end(int period){return END[period];}
    public static ScheduleRule normalize(ScheduleRule rule){
        Matcher label=LABEL.matcher(rule.getPeriods());
        if(!label.matches())return rule;
        int first=Integer.parseInt(label.group(1)),last=label.group(2)==null?first:Integer.parseInt(label.group(2));
        if(first<1||last>12||last<first)return rule;
        String oldStart=first==2?"08:50":first==4?"10:50":start(first);
        String oldEnd=last==1?"08:50":last==3?"10:50":end(last);
        // Only migrate the old standard clock, never arbitrary manually entered times.
        if(!rule.getStartTime().toString().equals(oldStart)||!rule.getEndTime().toString().equals(oldEnd))return rule;
        if(oldStart.equals(start(first))&&oldEnd.equals(end(last)))return rule;
        return new ScheduleRule(rule.getFromWeek(),rule.getToWeek(),rule.getDayOfWeek(),start(first),end(last),rule.getPeriods(),rule.getCourse(),rule.getRoom(),rule.getType(),rule.getTeacher(),rule.getId(),rule.getColor(),rule.getCredits(),rule.getNote());
    }
}
