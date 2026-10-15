package cn.nua.wenguan.schedule.ui;

import cn.nua.wenguan.schedule.data.SchoolPeriodTimes;
import java.time.LocalTime;

/** Occupancy is independent of visual padding, overlap lanes, or whether a card is clipped. */
public final class GridOccupancyPolicy {
    private final boolean[][] occupied=new boolean[7][12];
    public void reserve(int day,int first,int last){
        if(day<0||day>=7)return;
        for(int p=Math.max(1,first);p<=Math.min(12,last);p++)occupied[day][p-1]=true;
    }
    public void reserveTime(int day,LocalTime start,LocalTime end){
        if(start==null||end==null||!end.isAfter(start))return;
        for(int p=1;p<=12;p++)if(start.isBefore(SchoolPeriodTimes.endTime(p))&&SchoolPeriodTimes.startTime(p).isBefore(end))reserve(day,p,p);
    }
    public boolean free(int day,int first,int last){
        if(day<0||day>=7||first<1||last>12||last<first)return false;
        for(int p=first;p<=last;p++)if(occupied[day][p-1])return false;
        return true;
    }
}
