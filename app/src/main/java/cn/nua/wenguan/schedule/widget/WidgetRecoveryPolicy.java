package cn.nua.wenguan.schedule.widget;
/** Two delayed local redraws after an upgrade; never an endless periodic task. */
public final class WidgetRecoveryPolicy {
    public static final int ATTEMPTS=2;
    private WidgetRecoveryPolicy() { }
    public static long delay(int attempt){if(attempt==1)return 8000;if(attempt==2)return 25000;throw new IllegalArgumentException("Invalid retry");}
    public static boolean current(long expected,long actual,int attempt){return expected>0&&expected==actual&&attempt>=1&&attempt<=ATTEMPTS;}
}
