package cn.nua.wenguan.schedule.ui;

import cn.nua.wenguan.schedule.model.CourseSession;
import cn.nua.wenguan.schedule.data.SchoolPeriodTimes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** School periods, not the reference screenshot's different clock times. */
public final class PeriodGrid {
    public static final int COUNT = 12;
    private static final Pattern NUMBERS=Pattern.compile("\\d+");
    private PeriodGrid() { }
    public static String start(int period) { return SchoolPeriodTimes.start(period); }
    public static String end(int period) { return SchoolPeriodTimes.end(period); }
    public static int[] span(CourseSession s) {
        if ("自定义时间".equals(s.getPeriods())) return null;
        Matcher numbers = NUMBERS.matcher(s.getPeriods());
        int first = 0, last = 0;
        while (numbers.find()) {
            int value;
            try { value = Integer.parseInt(numbers.group()); } catch (NumberFormatException malformed) { return null; }
            if (value < 1 || value > COUNT) continue;
            if (first == 0) first = value;
            last = value;
        }
        if (first != 0 && last >= first) return new int[]{first, last};
        for (int i = 1; i <= COUNT; i++) {
            if (s.getStartTime().toString().equals(start(i))) first = i;
            if (s.getEndTime().toString().equals(end(i))) last = i;
        }
        // An unsupported custom time must never be silently placed in the wrong row.
        return first > 0 && last >= first ? new int[]{first, last} : null;
    }
    public static boolean overlaps(int[] a, int[] b) { return a[0] <= b[1] && b[0] <= a[1]; }
}
