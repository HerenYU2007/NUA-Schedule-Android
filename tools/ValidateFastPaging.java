import cn.nua.wenguan.schedule.ui.*;
import cn.nua.wenguan.schedule.data.SchoolPeriodTimes;
import java.time.*;
import java.util.*;

public final class ValidateFastPaging {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void close(float a,float b,String message){check(Math.abs(a-b)<.02f,message);}
    public static void main(String[] arguments){
        for(float width:new float[]{240,320,393,412,600,800,1024,1600})for(int step=-100;step<=100;step++){
            float offset=step*width/100f;int nearest=WeekPagerPolicy.nearest(offset,width);
            check(nearest==(Math.abs(step)>=50?(step<0?1:-1):0),"regrab adopts nearest visible week");
            float rebased=WeekPagerPolicy.rebased(offset,width,nearest);
            float[] before=WeekPagerPolicy.positions(width,offset),after=WeekPagerPolicy.positions(width,rebased);
            if(nearest==1){close(before[1],after[0],"current becomes previous with no jump");close(before[2],after[1],"next becomes current with no jump");}
            else if(nearest==-1){close(before[0],after[1],"previous becomes current with no jump");close(before[1],after[2],"current becomes next with no jump");}
            else for(int n=0;n<3;n++)close(before[n],after[n],"uncommitted regrab remains at visible position");
            for(int gesture=-1;gesture<=1;gesture++){
                check(WeekPagerPolicy.release(offset,width,gesture,true)==0,"multi-touch/cancel never navigates");
                int side=offset<0?1:offset>0?-1:0;
                check(WeekPagerPolicy.release(offset,width,gesture,false)==(gesture==0?nearest:gesture==side?side:0),"reversal settles within the visible pair");
            }
            close(after[0]+width,after[1],"previous/current remain tight");close(after[1]+width,after[2],"current/next remain tight");
        }
        LocalDate anchor=LocalDate.of(2026,10,15);
        check(WeekPrefetchPolicy.lookahead(anchor,1).equals(Arrays.asList(anchor.plusWeeks(1),anchor.minusWeeks(1),anchor.plusWeeks(2),anchor.minusWeeks(2),anchor.plusWeeks(3),anchor.minusWeeks(3))),"forward prefetch order");
        check(WeekPrefetchPolicy.lookahead(anchor,-1).get(0).equals(anchor.minusWeeks(1)),"reverse direction prepared first");
        for(LocalDate edge:new LocalDate[]{LocalDate.of(1900,1,1),LocalDate.of(2200,12,31)})for(LocalDate next:WeekPrefetchPolicy.lookahead(edge,1))check(next.getYear()>=1900&&next.getYear()<=2200,"lookahead respects date picker bounds");
        Map<LocalDate,String> cache=new LinkedHashMap<>();cache.put(anchor,"current");
        Random random=new Random(123);
        for(int i=0;i<10000;i++){
            LocalDate date=anchor.plusWeeks(random.nextInt(80)-40);cache.put(date,"week");WeekPrefetchPolicy.trim(cache,anchor);
            check(cache.size()<=7,"cache bounded under fast alternating traversal");check(cache.containsKey(anchor),"visible week never evicted");
        }
        cache.clear();for(int week=-3;week<=3;week++)cache.put(anchor.plusWeeks(week),"week");LocalDate moved=anchor.plusWeeks(1);cache.put(anchor.plusWeeks(4),"new next");WeekPrefetchPolicy.trim(cache,moved);
        check(!cache.containsKey(anchor.minusWeeks(3))&&cache.containsKey(anchor.plusWeeks(4)),"new forward lookahead not instantly evicted by LRU");
        for(int p=1;p<=12;p++){
            check(SchoolPeriodTimes.startTime(p).equals(LocalTime.parse(SchoolPeriodTimes.start(p))),"cached start clock exact");check(SchoolPeriodTimes.endTime(p).equals(LocalTime.parse(SchoolPeriodTimes.end(p))),"cached end clock exact");
        }
        System.out.println("Fast paging: "+checks+" checks passed (continuous regrab geometry, reversals/cancellation, bounded priority prefetch, unchanged clock). No live FPS claim.");
    }
}
