package cn.nua.wenguan.schedule.data;
import cn.nua.wenguan.schedule.model.ScheduleRule;
import java.util.regex.*;

/** Shared, validated school clock. Explicit custom course times remain literal. */
public final class SchoolPeriodTimes {
    private static final String[] START={"","08:00","08:55","10:00","10:55","14:00","14:55","16:00","16:55","18:30","19:25","20:30","21:25"};
    private static final String[] END={"","08:45","09:40","10:45","11:40","14:45","15:40","16:45","17:40","19:15","20:10","21:15","22:00"};
    private static final String[] PREVIOUS_START={"","08:00","08:55","10:00","10:55","14:00","14:55","16:00","16:50","18:30","19:20","20:30","21:15"};
    private static final String[] PREVIOUS_END={"","08:45","09:40","10:45","11:40","14:45","15:40","16:45","17:40","19:20","20:10","21:15","22:00"};
    /** Only an unversioned, entirely unchanged previous default is upgraded. */
    public static String[][] upgradePreviousDefault(String[] starts,String[] ends){
        validate(starts,ends);
        return java.util.Arrays.equals(starts,PREVIOUS_START)&&java.util.Arrays.equals(ends,PREVIOUS_END)
            ?new String[][]{defaultStarts(),defaultEnds()}:new String[][]{starts.clone(),ends.clone()};
    }
    private static final class Clock {
        final String[] starts,ends;final java.time.LocalTime[] startTimes=new java.time.LocalTime[13],endTimes=new java.time.LocalTime[13];final String revision;
        Clock(String[] starts,String[] ends){this.starts=starts.clone();this.ends=ends.clone();for(int p=1;p<=12;p++){startTimes[p]=java.time.LocalTime.parse(starts[p]);endTimes[p]=java.time.LocalTime.parse(ends[p]);}revision=String.join(",",starts)+"/"+String.join(",",ends);}
    }
    private static volatile Clock clock=new Clock(START,END);
    public static String[] defaultStarts(){return START.clone();}
    public static String[] defaultEnds(){return END.clone();}
    public static String[] starts(){return clock.starts.clone();}
    public static String[] ends(){return clock.ends.clone();}
    public static String revision(){return clock.revision;}
    public static void validate(String[] starts,String[] ends){
        if(starts==null||ends==null||starts.length!=13||ends.length!=13)throw new IllegalArgumentException("需要完整的12节时间");
        for(int p=1;p<=12;p++){
            if(starts[p]==null||ends[p]==null||!starts[p].matches("(?:[01]\\d|2[0-3]):[0-5]\\d")||!ends[p].matches("(?:[01]\\d|2[0-3]):[0-5]\\d")||starts[p].compareTo(ends[p])>=0)
                throw new IllegalArgumentException("第"+p+"节结束时间须晚于开始时间");
            if(p>1&&ends[p-1].compareTo(starts[p])>0)throw new IllegalArgumentException("第"+(p-1)+"节与第"+p+"节时间重叠");
        }
    }
    public static synchronized void configure(String[] starts,String[] ends){validate(starts,ends);if(!java.util.Arrays.equals(starts,clock.starts)||!java.util.Arrays.equals(ends,clock.ends))clock=new Clock(starts,ends);}
    public static boolean matchesClock(String periods,String start,String end){
        if(periods==null||start==null||end==null)return false;
        Matcher label=LABEL.matcher(periods);if(!label.matches())return false;
        int first=Integer.parseInt(label.group(1)),last=label.group(2)==null?first:Integer.parseInt(label.group(2));
        if(first<1||last>12||last<first)return false;
        String legacyStart=first==2?"08:50":first==4?"10:50":first==6?"14:50":PREVIOUS_START[first];
        String legacyEnd=last==1?"08:50":last==3?"10:50":last==5?"14:50":last==7?"16:50":PREVIOUS_END[last];
        String morningStart=first==6?"14:50":PREVIOUS_START[first],morningEnd=last==5?"14:50":last==7?"16:50":PREVIOUS_END[last];
        return (start.equals(start(first))&&end.equals(end(last)))||(start.equals(START[first])&&end.equals(END[last]))
            ||(start.equals(PREVIOUS_START[first])&&end.equals(PREVIOUS_END[last]))
            ||(start.equals(morningStart)&&end.equals(morningEnd))||(start.equals(legacyStart)&&end.equals(legacyEnd));
    }
    private static final Pattern LABEL=Pattern.compile("第?\\s*(\\d{1,2})(?:\\s*[-—–~至]\\s*(\\d{1,2}))?\\s*节");
    public static java.time.LocalTime startTime(int period){return clock.startTimes[period];}
    public static java.time.LocalTime endTime(int period){return clock.endTimes[period];}
    private SchoolPeriodTimes() { }
    public static String start(int period){return clock.starts[period];}
    public static String end(int period){return clock.ends[period];}
    public static ScheduleRule normalize(ScheduleRule rule){
        Matcher label=LABEL.matcher(rule.getPeriods());
        if(!label.matches())return rule;
        int first=Integer.parseInt(label.group(1)),last=label.group(2)==null?first:Integer.parseInt(label.group(2));
        if(first<1||last>12||last<first)return rule;
        if(!rule.followsPeriodClock())return rule;
        if(rule.getStartTime().toString().equals(start(first))&&rule.getEndTime().toString().equals(end(last)))return rule;
        return new ScheduleRule(rule.getFromWeek(),rule.getToWeek(),rule.getDayOfWeek(),start(first),end(last),rule.getPeriods(),rule.getCourse(),rule.getRoom(),rule.getType(),rule.getTeacher(),rule.getId(),rule.getColor(),rule.getCredits(),rule.getNote(),true);
    }
}
