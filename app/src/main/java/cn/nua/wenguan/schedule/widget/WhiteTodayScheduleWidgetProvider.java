package cn.nua.wenguan.schedule.widget;
import android.content.Context;
/** Separate launcher entry and date selection; layout sizes are shared with the original. */
public final class WhiteTodayScheduleWidgetProvider extends TodayScheduleWidgetProvider {
    @Override protected boolean white(){return true;}
    @Override protected String preferencesName(){return "white_widget_preferences";}
    public static void refreshAll(Context c){new WhiteTodayScheduleWidgetProvider().updateAll(c);}
}
