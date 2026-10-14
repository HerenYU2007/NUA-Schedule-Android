package cn.nua.wenguan.schedule.widget;

/** Launcher options can be missing, inverted, or stale during a resize. All units are dp. */
public final class WidgetSizePolicy {
    private WidgetSizePolicy() { }
    public static int dimension(int value, int fallback) { return value > 0 ? value : fallback; }
    public static int[] range(int minimum, int maximum, int fallback) {
        minimum = dimension(minimum, fallback);
        maximum = dimension(maximum, minimum);
        return new int[]{Math.min(minimum, maximum), Math.max(minimum, maximum)};
    }
    public static boolean current(float width, float height, int minWidth, int maxWidth, int minHeight, int maxHeight) {
        if (!Float.isFinite(width) || !Float.isFinite(height) || width <= 0 || height <= 0) return false;
        // Only constrain dimensions for which the host supplied a complete valid range.
        boolean widthOk = minWidth <= 0 || maxWidth < minWidth || maxWidth <= 0
                || (width >= minWidth - 2 && width <= maxWidth + 2);
        boolean heightOk = minHeight <= 0 || maxHeight < minHeight || maxHeight <= 0
                || (height >= minHeight - 2 && height <= maxHeight + 2);
        return widthOk && heightOk;
    }
}
