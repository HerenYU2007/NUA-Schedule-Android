package cn.nua.wenguan.schedule.ui;

import cn.nua.wenguan.schedule.model.CourseSession;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** School periods, not the reference screenshot's different clock times. */
public final class PeriodGrid {
    public static final int COUNT = 12;
    private static final String[] START = {"", "08:00", "08:50", "10:00", "10:50", "14:00", "14:50", "16:00", "16:50", "18:30", "19:20", "20:30", "21:15"};
    private static final String[] END = {"", "08:50", "09:40", "10:50", "11:40", "14:50", "15:40", "16:50", "17:40", "19:20", "20:10", "21:15", "22:00"};
    private PeriodGrid() { }
    public static String start(int period) { return START[period]; }
    public static String end(int period) { return END[period]; }
    public static int[] span(CourseSession s) {
        Matcher numbers = Pattern.compile("\\d+").matcher(s.getPeriods());
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
            if (s.getStartTime().toString().equals(START[i])) first = i;
            if (s.getEndTime().toString().equals(END[i])) last = i;
        }
        // An unsupported custom time must never be silently placed in the wrong row.
        return first > 0 && last >= first ? new int[]{first, last} : null;
    }
    public static boolean overlaps(int[] a, int[] b) { return a[0] <= b[1] && b[0] <= a[1]; }
}
