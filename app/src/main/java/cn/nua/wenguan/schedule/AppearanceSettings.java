package cn.nua.wenguan.schedule;

import android.content.Context;
import android.content.res.Configuration;

/** UI preferences are independent of course data and login settings. */
public final class AppearanceSettings {
    private AppearanceSettings() { }
    public static int theme(Context c) {
        int value = c.getSharedPreferences("appearance", 0).getInt("theme", 0);
        return value >= 0 && value <= 2 ? value : 0;
    }
    public static boolean saveTheme(Context c, int value) {
        return value >= 0 && value <= 2 && c.getSharedPreferences("appearance", 0).edit().putInt("theme", value).commit();
    }
    public static boolean grid(Context c) { return c.getSharedPreferences("appearance", 0).getBoolean("grid", false); }
    public static boolean saveGrid(Context c, boolean grid) { return c.getSharedPreferences("appearance", 0).edit().putBoolean("grid", grid).commit(); }
    public static String themeLabel(Context c) { return new String[]{"跟随系统", "浅色", "深色"}[theme(c)]; }
    public static Context wrap(Context c) {
        int choice = theme(c);
        if (choice == 0) return c;
        Configuration config = new Configuration(c.getResources().getConfiguration());
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | (choice == 2 ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        return c.createConfigurationContext(config);
    }
    public static boolean dark(Context c) {
        return (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }
}
