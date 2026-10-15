package cn.nua.wenguan.schedule.ui;
import cn.nua.wenguan.schedule.data.SchoolPeriodTimes;
import java.time.*;
import java.time.temporal.ChronoUnit;

/** Pure geometry and draft bounds; never writes schedule/agenda data. */
public final class GridPlacementPolicy {
    public static final int MOVE=0,START=1,END=2;
    private GridPlacementPolicy() { }
    public static int[] hit(float x,float y,int axis,int header,int column,int row){
        if(!Float.isFinite(x)||!Float.isFinite(y)||column<1||row<1||x<axis||y<header)return null;
        int day=(int)((x-axis)/column),period=(int)((y-header)/row)+1;
        return day>=0&&day<7&&period>=1&&period<=12?new int[]{day,period}:null;
    }
    public static int[] drag(int first,int last,int delta,int kind){
        if(first<1||last>12||last<first)throw new IllegalArgumentException("Invalid draft span");
        if(kind==START)return new int[]{Math.max(1,Math.min(last,first+delta)),last};
        if(kind==END)return new int[]{first,Math.max(first,Math.min(12,last+delta))};
        int length=last-first,next=Math.max(1,Math.min(12-length,first+delta));
        return new int[]{next,next+length};
    }
    public static int week(LocalDate date,LocalDate schoolStart){
        LocalDate a=schoolStart.minusDays(schoolStart.getDayOfWeek().getValue()-1L);
        LocalDate b=date.minusDays(date.getDayOfWeek().getValue()-1L);
        return Math.toIntExact(ChronoUnit.WEEKS.between(a,b)+1);
    }
    public static float position(LocalTime time){
        for(int p=1;p<=12;p++){
            LocalTime start=SchoolPeriodTimes.startTime(p),end=SchoolPeriodTimes.endTime(p);
            if(time.isBefore(start))return p-1;
            if(!time.isAfter(end))return p-1+(float)ChronoUnit.SECONDS.between(start,time)/ChronoUnit.SECONDS.between(start,end);
        }
        return 12;
    }
    public static float[] agendaSpan(LocalTime start,LocalTime end){
        if(start==null||end==null||!end.isAfter(start)||start.isBefore(LocalTime.of(8,0))||end.isAfter(LocalTime.of(22,0)))return null;
        float first=position(start),last=position(end);
        if(last-first<.0001f)return null; // Entirely in a break: show separately, never put in a false period.
        float bottom=Math.min(12,Math.max(last,first+.35f));
        return new float[]{Math.max(0,Math.min(first,bottom-.35f)),bottom};
    }
}
