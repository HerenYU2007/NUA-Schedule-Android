package cn.nua.wenguan.schedule;
import android.content.*;
import cn.nua.wenguan.schedule.model.CourseSession;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class ReminderPreferences {
    private ReminderPreferences() { }
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("reminders",0);}
    private static String course(String name){return "course_"+UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));}
    private static String lesson(CourseSession s){String id=s.getRuleId().isEmpty()?ReminderScheduler.identity(s):s.getDate()+"|"+s.getRuleId();return "lesson_"+UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8));}
    public static int defaultMode(Context c){return prefs(c).getInt("mode",1);}
    public static int mode(Context c,CourseSession s){SharedPreferences p=prefs(c);return ReminderPolicy.effectiveMode(p.getBoolean("enabled",false),p.getInt(lesson(s),-1),p.getInt(course(s.getCourse()),-1),p.getBoolean("selected_only",false),p.getStringSet("selected",Collections.emptySet()).contains(s.getCourse()),defaultMode(c));}
    public static int overrideMode(Context c,CourseSession s,boolean single){return prefs(c).getInt(single?lesson(s):course(s.getCourse()),-1);}
    public static int overrideMinutes(Context c,CourseSession s,boolean single){
        SharedPreferences p=prefs(c);String key=single?lesson(s):course(s.getCourse());
        int fallback=single?p.getInt(course(s.getCourse())+"_minutes",ReminderScheduler.minutesBefore(c)):ReminderScheduler.minutesBefore(c);
        return Math.max(0,Math.min(120,p.getInt(key+"_minutes",fallback)));
    }
    public static int minutes(Context c,CourseSession s){SharedPreferences p=prefs(c);return Math.max(0,Math.min(120,p.getInt(lesson(s)+"_minutes",p.getInt(course(s.getCourse())+"_minutes",ReminderScheduler.minutesBefore(c)))));}
    public static boolean configure(Context c,boolean enabled,int minutes,int mode,boolean selectedOnly,Set<String> selected){return prefs(c).edit().putBoolean("enabled",enabled).putInt("minutes",minutes).putInt("mode",mode).putBoolean("selected_only",selectedOnly).putStringSet("selected",new HashSet<>(selected)).commit();}
    public static boolean override(Context c,CourseSession s,boolean single,int mode,int minutes){if(mode< -1||mode>2||minutes<0||minutes>120)return false;String key=single?lesson(s):course(s.getCourse());SharedPreferences.Editor e=prefs(c).edit();if(mode<0)e.remove(key).remove(key+"_minutes");else e.putInt(key,mode).putInt(key+"_minutes",minutes);return e.commit();}
    public static void renameCourse(Context c,String oldName,String newName){renameCourse(c,oldName,newName,false);}
    public static void renameCourse(Context c,String oldName,String newName,boolean keepOriginal){
        newName=newName.trim();if(oldName.isEmpty()||oldName.equals(newName)||newName.isEmpty())return;
        SharedPreferences p=prefs(c);Set<String> selected=new HashSet<>(p.getStringSet("selected",Collections.emptySet()));
        if(selected.contains(oldName)){selected.add(newName);if(!keepOriginal)selected.remove(oldName);}
        SharedPreferences.Editor e=p.edit().putStringSet("selected",selected);
        String old=course(oldName),next=course(newName);
        if(p.contains(old)&&!p.contains(next))e.putInt(next,p.getInt(old,-1)).putInt(next+"_minutes",p.getInt(old+"_minutes",ReminderScheduler.minutesBefore(c)));
        if(!keepOriginal)e.remove(old).remove(old+"_minutes");
        e.commit();
    }
    public static boolean selectedOnly(Context c){return prefs(c).getBoolean("selected_only",false);}
    public static Set<String> selected(Context c){return new HashSet<>(prefs(c).getStringSet("selected",Collections.emptySet()));}
    public static String label(int mode){return mode==2?"闹钟响铃":mode==1?"通知提醒":mode==0?"不提醒":"跟随全局";}
}
