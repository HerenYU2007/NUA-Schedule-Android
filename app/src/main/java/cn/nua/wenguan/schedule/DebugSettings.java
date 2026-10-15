package cn.nua.wenguan.schedule;
import android.content.Context;
/** Controls extra sync UI only; never enables remote WebView inspection or credential logging. */
public final class DebugSettings {
    private DebugSettings() { }
    public static boolean enabled(Context c){
        try{return c.getSharedPreferences("interface_debug",Context.MODE_PRIVATE).getBoolean("enabled",false);}
        catch(ClassCastException invalid){return false;}
    }
    public static boolean save(Context c,boolean enabled){
        return c.getSharedPreferences("interface_debug",Context.MODE_PRIVATE).edit().putBoolean("enabled",enabled).commit();
    }
}
