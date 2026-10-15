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
        String[] starts=s.split(",",-1),ends=e.split(",",-1);
        if(prefs.getInt("revision",1)<2){String[][] upgraded=SchoolPeriodTimes.upgradePreviousDefault(starts,ends);starts=upgraded[0];ends=upgraded[1];}
        SchoolPeriodTimes.configure(starts,ends);
    }
    public static synchronized boolean save(Context context,String[] starts,String[] ends){
        SchoolPeriodTimes.validate(starts,ends);
        SharedPreferences prefs=context.getSharedPreferences("period_clock",0);
        String oldS=prefs.getString("starts",null),oldE=prefs.getString("ends",null);
        int oldRevision=prefs.getInt("revision",0);
        if(!prefs.edit().putString("starts",String.join(",",starts)).putString("ends",String.join(",",ends)).putInt("revision",2).commit()){
            SharedPreferences.Editor rollback=prefs.edit();
            if(oldS==null)rollback.remove("starts");else rollback.putString("starts",oldS);
            if(oldE==null)rollback.remove("ends");else rollback.putString("ends",oldE);
            if(oldRevision==0)rollback.remove("revision");else rollback.putInt("revision",oldRevision);
            rollback.commit();return false;
        }
        SchoolPeriodTimes.configure(starts,ends);return true;
    }
}
