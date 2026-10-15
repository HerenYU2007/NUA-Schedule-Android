package cn.nua.wenguan.schedule;
/** Pure reminder decisions for regression tests; all times are epoch milliseconds. */
public final class ReminderPolicy {
    private ReminderPolicy() { }
    public static int effectiveMode(boolean enabled,int lesson,int course,boolean selectedOnly,boolean selected,int defaultMode){if(!enabled)return 0;if(lesson>=0&&lesson<=2)return lesson;if(course>=0&&course<=2)return course;if(selectedOnly&&!selected)return 0;return defaultMode==2?2:1;}
    public static boolean eligible(long now,long end,long due,int mode,boolean fired){return mode>0&&mode<=2&&!fired&&now<end&&now-due<=(mode==2?120000L:1800000L);}
}
