import cn.nua.wenguan.schedule.ui.*;
import java.time.*;
import java.util.*;
public final class ValidateGridInteractions {
    private static int checks;
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    public static void main(String[] args){
        for(int day=0;day<7;day++)for(int p=1;p<=12;p++)check(Arrays.equals(GridPlacementPolicy.hit(48+day*40+20,56+(p-1)*82+41,48,56,40,82),new int[]{day,p}),"blank hit day/period");
        for(float[] outside:new float[][]{{47,80},{70,55},{328,80},{70,1040},{Float.NaN,80},{70,Float.POSITIVE_INFINITY}})check(GridPlacementPolicy.hit(outside[0],outside[1],48,56,40,82)==null,"axis/header/edges rejected");
        for(int first=1;first<=12;first++)for(int last=first;last<=12;last++)for(int delta=-20;delta<=20;delta++)for(int kind=0;kind<3;kind++){
            int[] span=GridPlacementPolicy.drag(first,last,delta,kind);
            check(span[0]>=1&&span[1]<=12&&span[0]<=span[1],"draft in bounds");
            if(kind==0)check(span[1]-span[0]==last-first,"moving preserves duration");
            if(kind==1)check(span[1]==last,"top handle keeps bottom");
            if(kind==2)check(span[0]==first,"bottom handle keeps top");
        }
        LocalDate opening=LocalDate.of(2026,9,9);
        for(int day=7;day<=13;day++)check(GridPlacementPolicy.week(LocalDate.of(2026,9,day),opening)==1,"chosen opening week");
        check(GridPlacementPolicy.week(LocalDate.of(2026,9,14),opening)==2,"next week prefill");
        check(GridPlacementPolicy.week(LocalDate.of(2026,9,6),opening)==0,"before semester not silently clamped");
        check(GridPlacementPolicy.agendaSpan(LocalTime.of(12,10),LocalTime.of(13,40))==null,"lunch shown separately");
        check(GridPlacementPolicy.agendaSpan(LocalTime.of(7,0),LocalTime.of(8,30))==null,"outside school hours shown separately");
        check(GridPlacementPolicy.agendaSpan(LocalTime.of(21,0),LocalTime.of(23,0))==null,"late event shown separately");
        float[] morning=GridPlacementPolicy.agendaSpan(LocalTime.of(8,55),LocalTime.of(11,40));
        check(morning[0]==1&&morning[1]==4,"agenda uses new morning clock and correct rows");
        float[] tiny=GridPlacementPolicy.agendaSpan(LocalTime.of(21,59),LocalTime.of(22,0));
        check(tiny[0]>=0&&tiny[1]<=12&&tiny[1]-tiny[0]>=.349f,"short event remains clickable");
        for(int minute=8*60;minute<22*60;minute++)check(GridPlacementPolicy.position(LocalTime.of(minute/60,minute%60))<=GridPlacementPolicy.position(LocalTime.of((minute+1)/60,(minute+1)%60)),"time placement monotonic");
        check(WeekSwipePolicy.direction(-100,85,48)==1,"diagonal left accepted");
        check(WeekSwipePolicy.direction(100,-100,48)==-1,"45-degree right accepted");
        check(WeekSwipePolicy.direction(-30,150,48)==0,"vertical retained");
        check(WeekSwipePolicy.lock(5,10,8)==0,"initial diagonal jitter undecided");
        check(WeekSwipePolicy.lock(-30,25,8)==1,"horizontal intent despite tilt");
        check(WeekSwipePolicy.lock(20,50,8)==2,"strong vertical intent");
        System.out.println("Grid interactions: "+checks+" checks passed (blank mapping, move/resize bounds, week prefill, agenda geometry, diagonal intent). Device drag/animation still required.");
    }
}
