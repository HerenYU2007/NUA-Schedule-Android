package cn.nua.wenguan.schedule.widget;
import android.content.Context;
/** Separate white weekly widget; actions retain this provider and its widget ID. */
public final class WhiteWeekScheduleWidgetProvider extends WeekScheduleWidgetProvider {
    public static void refreshAll(Context c){refreshProvider(c,WhiteWeekScheduleWidgetProvider.class);}
}
