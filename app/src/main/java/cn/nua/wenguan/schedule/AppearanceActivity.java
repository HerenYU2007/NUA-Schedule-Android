package cn.nua.wenguan.schedule;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

public abstract class AppearanceActivity extends Activity {
    private int appliedTheme;
    @Override protected void attachBaseContext(Context base) { super.attachBaseContext(AppearanceSettings.wrap(base)); }
    @Override protected void onCreate(Bundle state) {
        appliedTheme = AppearanceSettings.theme(this);
        super.onCreate(state);
        try { cn.nua.wenguan.schedule.data.PeriodTimeStore.load(this); }
        catch(RuntimeException unavailable) { android.widget.Toast.makeText(this,"节次时间设置读取失败，请在设置中检查",android.widget.Toast.LENGTH_LONG).show(); }
        cn.nua.wenguan.schedule.ui.UiPerformance.configure(getWindow());
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            android.view.View decor = getWindow().getDecorView();
            int flags = decor.getSystemUiVisibility();
            decor.setSystemUiVisibility(AppearanceSettings.dark(this)
                    ? flags & ~android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    : flags | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        } else {
            getWindow().setNavigationBarColor(getColor(R.color.header_background));
        }
    }
    @Override protected void onResume() {
        super.onResume();
        if (appliedTheme != AppearanceSettings.theme(this)) recreate();
    }
}
