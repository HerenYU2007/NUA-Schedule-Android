package cn.nua.wenguan.schedule.ui;
import android.os.Build;
import android.view.Window;
/** Let Android boost touch responsiveness; never force a permanent display mode or CPU lock. */
public final class UiPerformance {
    private UiPerformance() { }
    public static void configure(Window window){
        if(window!=null&&Build.VERSION.SDK_INT>=35)window.setFrameRateBoostOnTouchEnabled(true);
    }
}
