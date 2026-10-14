package cn.nua.wenguan.schedule.ui;
public final class WeekSwipePolicy {
    private WeekSwipePolicy() { }
    public static int direction(float dx, float dy, float threshold) {
        if (!Float.isFinite(dx) || !Float.isFinite(dy) || Math.abs(dx) < threshold || Math.abs(dx) < Math.abs(dy) * 1.5f) return 0;
        return dx < 0 ? 1 : -1;
    }
}
