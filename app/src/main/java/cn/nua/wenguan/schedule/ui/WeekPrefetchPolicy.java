package cn.nua.wenguan.schedule.ui;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Bounded lookahead: nearest pages before other work, recently travelled direction first. */
public final class WeekPrefetchPolicy {
    public static final int CACHE_SIZE=7;
    private WeekPrefetchPolicy() { }
    public static List<LocalDate> lookahead(LocalDate anchor,int direction){
        int first=direction<0?-1:1;List<LocalDate> result=new ArrayList<>(6);
        for(int distance=1;distance<=3;distance++)for(int sign:new int[]{first,-first}){
            LocalDate date=anchor.plusWeeks((long)distance*sign);if(date.getYear()>=1900&&date.getYear()<=2200)result.add(date);
        }
        return result;
    }
    public static <T> void trim(Map<LocalDate,T> cache,LocalDate anchor){
        while(cache.size()>CACHE_SIZE){
            LocalDate farthest=null;long distance=-1;
            for(LocalDate key:cache.keySet())if(!key.equals(anchor)){
                long days=Math.abs(ChronoUnit.DAYS.between(anchor,key));if(days>distance){distance=days;farthest=key;}
            }
            if(farthest==null)break;cache.remove(farthest);
        }
    }
}
