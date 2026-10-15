package cn.nua.wenguan.schedule.data;
import android.content.Context;
import android.content.SharedPreferences;
/** Separate preference transaction; never overwrites courses or login data. */
public final class PeriodTimeStore {
    private PeriodTimeStore(){}
    public static synchronized void load(Context context){
        SharedPreferences prefs=context.getSharedPreferences("period_clock",0);
        String s=prefs.getString("starts",null),e=prefs.getString("ends",null);
        if(s==null&&e==null){SchoolPeriodTimes.configure(SchoolPeriodTimes.defaultStarts(),SchoolPeriodTimes.defaultEnds());return;}
        if(s==null||e==null)throw new IllegalStateException("节次时间读取失败，原设置保留");
        SchoolPeriodTimes.configure(s.split(",",-1),e.split(",",-1));
    }
    public static synchronized boolean save(Context context,String[] starts,String[] ends){
        SchoolPeriodTimes.validate(starts,ends);
        SharedPreferences prefs=context.getSharedPreferences("period_clock",0);
        String oldS=prefs.getString("starts",null),oldE=prefs.getString("ends",null);
        if(!prefs.edit().putString("starts",String.join(",",starts)).putString("ends",String.join(",",ends)).commit()){
            SharedPreferences.Editor rollback=prefs.edit();
            if(oldS==null)rollback.remove("starts");else rollback.putString("starts",oldS);
            if(oldE==null)rollback.remove("ends");else rollback.putString("ends",oldE);
            rollback.commit();return false;
        }
        SchoolPeriodTimes.configure(starts,ends);return true;
    }
}
