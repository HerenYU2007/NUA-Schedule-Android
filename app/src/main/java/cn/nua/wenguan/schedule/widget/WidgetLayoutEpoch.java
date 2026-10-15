package cn.nua.wenguan.schedule.widget;

import android.content.Context;
import cn.nua.wenguan.schedule.R;

/** Alternate root layout identities on APK replacement: do not reapply into a stale host tree. */
public final class WidgetLayoutEpoch {
    private static int cached=-1;
    private WidgetLayoutEpoch() { }
    private static long installed(Context c){try{return c.getPackageManager().getPackageInfo(c.getPackageName(),0).lastUpdateTime;}catch(android.content.pm.PackageManager.NameNotFoundException missing){return 0;}}
    private static synchronized int slot(Context c){
        if(cached>=0)return cached;
        android.content.SharedPreferences p=c.getSharedPreferences("widget_layout_epoch",0);
        long installed=installed(c);int previous=p.getInt("slot",0);
        cached=installed!=p.getLong("installed",-1)?1-previous:previous;
        p.edit().putLong("installed",installed).putInt("slot",cached).apply();return cached;
    }
    public static synchronized void force(Context c){int next=1-slot(c);cached=next;c.getSharedPreferences("widget_layout_epoch",0).edit().putLong("installed",installed(c)).putInt("slot",next).apply();}
    public static int layout(Context c,int base){
        boolean second=slot(c)==1;
        if(base==R.layout.widget_phone)return second?R.layout.widget_phone_b:R.layout.widget_phone_a;
        if(base==R.layout.widget_today)return second?R.layout.widget_today_b:R.layout.widget_today_a;
        if(base==R.layout.widget_week)return second?R.layout.widget_week_b:R.layout.widget_week_a;
        if(base==R.layout.widget_phone_white)return second?R.layout.widget_phone_white_b:R.layout.widget_phone_white_a;
        if(base==R.layout.widget_today_white)return second?R.layout.widget_today_white_b:R.layout.widget_today_white_a;
        if(base==R.layout.widget_week_white)return second?R.layout.widget_week_white_b:R.layout.widget_week_white_a;
        throw new IllegalArgumentException("Unsupported widget layout");
    }
}
