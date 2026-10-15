import cn.nua.wenguan.schedule.widget.*;
import java.time.*;
public final class ValidateWidgetRecovery {
    private static int count;
    private static void check(boolean ok,String message){count++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        LocalDate today=LocalDate.of(2026,10,15);
        for(int hour=0;hour<24;hour++){
            check(DailyWidgetSelectionPolicy.offset(true,1,today,today,hour)==1,"upgrading keeps manual tomorrow");
            check(DailyWidgetSelectionPolicy.offset(true,0,today,today,hour)==0,"upgrading keeps manual today, even after22");
            check(DailyWidgetSelectionPolicy.offset(true,1,null,today,hour)==1,"legacy no date stamp retains choice");
            check(DailyWidgetSelectionPolicy.offset(false,0,null,today,hour)==(hour>=22?1:0),"new widget correct date");
            check(DailyWidgetSelectionPolicy.offset(true,1,today.minusDays(1),today,hour)==(hour>=22?1:0),"missed midnight repaired");
        }
        check(WidgetRecoveryPolicy.ATTEMPTS==2,"only two delayed refreshes");
        check(WidgetRecoveryPolicy.delay(1)==8000&&WidgetRecoveryPolicy.delay(2)==25000,"bounded delay intent");
        for(int attempt=-2;attempt<6;attempt++){
            check(WidgetRecoveryPolicy.current(1,1,attempt)==(attempt>=1&&attempt<=2),"only valid attempt range");
            check(!WidgetRecoveryPolicy.current(1,2,attempt),"obsolete upgrade job ignored");
            check(!WidgetRecoveryPolicy.current(0,0,attempt),"uninitialized job ignored");
        }
        for(int attempt:new int[]{0,3})try{WidgetRecoveryPolicy.delay(attempt);throw new AssertionError("unbounded delay accepted");}catch(IllegalArgumentException expected){count++;}
        System.out.println("Widget recovery: "+count+" checks passed (same-day/legacy manual selection, midnight, bounded/stale job policy). Upgrade with real launcher still needs device.");
    }
}
