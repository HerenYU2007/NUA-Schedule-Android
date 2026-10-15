package cn.nua.wenguan.schedule.widget;
import java.time.LocalDate;
/** Same-day upgrades preserve manual Today/Tomorrow selection; missed midnight is repaired. */
public final class DailyWidgetSelectionPolicy {
    private DailyWidgetSelectionPolicy() { }
    public static int offset(boolean hasChoice,int stored,LocalDate chosenOn,LocalDate today,int hour){
        if(!hasChoice||(chosenOn!=null&&!chosenOn.equals(today)))return hour>=22?1:0;
        return stored==1?1:0;
    }
}
