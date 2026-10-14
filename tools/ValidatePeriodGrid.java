import cn.nua.wenguan.schedule.model.CourseSession;
import cn.nua.wenguan.schedule.ui.PeriodGrid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;

public final class ValidatePeriodGrid {
    private static CourseSession session(String label, String start, String end) {
        return new CourseSession(LocalDate.of(2026,10,14), LocalTime.parse(start), LocalTime.parse(end), label, "测试课程", "文313", "理论", "测试教师");
    }
    private static void check(boolean result, String message) { if (!result) throw new AssertionError(message); }
    public static void main(String[] args) {
        check(Arrays.equals(PeriodGrid.span(session("第2—4节", "08:50", "11:40")), new int[]{2,4}), "2-4 aligns with rows, not duplicated across rows");
        check(Arrays.equals(PeriodGrid.span(session("第1-2节", "08:00", "09:40")), new int[]{1,2}), "morning span");
        check(Arrays.equals(PeriodGrid.span(session("第11-12节", "20:30", "22:00")), new int[]{11,12}), "late-night courses retained");
        check(Arrays.equals(PeriodGrid.span(session("未标注", "14:00", "15:40")), new int[]{5,6}), "exact clock-time fallback");
        check(PeriodGrid.span(session("未标注", "12:10", "13:40")) == null, "custom lunch not silently put into period1");
        check(PeriodGrid.span(session("第99999999999999999999节", "12:10", "13:40")) == null, "malformed labels cannot crash");
        check(PeriodGrid.overlaps(new int[]{2,4}, new int[]{4,6}), "collision inclusive");
        check(!PeriodGrid.overlaps(new int[]{2,4}, new int[]{5,6}), "consecutive blocks not collision");
        check("14:00".equals(PeriodGrid.start(5)) && "15:40".equals(PeriodGrid.end(6)), "school times, not screenshot times");
        System.out.println("Period grid tests passed: range, night, exact-time fallback, unsupported custom time and collisions.");
    }
}
