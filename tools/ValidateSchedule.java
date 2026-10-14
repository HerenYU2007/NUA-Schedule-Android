import cn.nua.wenguan.schedule.data.ScheduleRepository;
import cn.nua.wenguan.schedule.model.CourseStatus;
import cn.nua.wenguan.schedule.model.ScheduleRule;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

public final class ValidateSchedule {
    public static void main(String[] args) {
        require(!ScheduleRepository.hasImportedRules(), "首次运行应为空课表");
        require(ScheduleRepository.totalSessionCount() == 0, "默认课次必须为0");
        require(ScheduleRepository.sessionsOn(LocalDate.of(2026, 10, 8)).isEmpty(), "不能带入旧课表");
        require(ScheduleRepository.sessionsOn(ScheduleRepository.today()).isEmpty(), "今天默认无课程数据");
        require(ScheduleRepository.sessionsInWeek(5).isEmpty(), "第5周不再有内置课程");
        require(ScheduleRepository.weekNumber(ScheduleRepository.today()) == 0, "未设置学期时不显示推算周次");
        require(!ScheduleRepository.isVisibleWeek(1), "空课表没有有效周次范围");

        ScheduleRule imported = new ScheduleRule(1, 20, DayOfWeek.MONDAY, "08:00", "09:40",
                "第1–2节", "测试课程", "测试教室", "理论", "教师");
        LocalDate anchor = LocalDate.of(2027, 2, 22);
        ScheduleRepository.setImportedRules(List.of(imported), anchor);
        require(ScheduleRepository.totalSessionCount() == 20, "导入课程应正常显示");
        require(ScheduleRepository.weekNumber(anchor) == 1, "按导入学期计算周次");
        var session = ScheduleRepository.sessionsOn(anchor).get(0);
        require(ScheduleRepository.statusAt(session, ZonedDateTime.parse("2027-02-22T07:30:00+08:00")) == CourseStatus.UPCOMING, "上课前");
        require(ScheduleRepository.statusAt(session, ZonedDateTime.parse("2027-02-22T08:30:00+08:00")) == CourseStatus.IN_PROGRESS, "上课中");
        require(ScheduleRepository.statusAt(session, ZonedDateTime.parse("2027-02-22T09:40:00+08:00")) == CourseStatus.ENDED, "结束时刻应隐藏");
        for (int offset = 0; offset < 7; offset++) {
            LocalDate chosen = anchor.plusDays(offset);
            ScheduleRepository.setImportedRules(List.of(imported), chosen);
            require(ScheduleRepository.schoolStartDate().equals(chosen), "保留具体开学日期");
            require(ScheduleRepository.weekOneMonday().equals(anchor), "任意星期开学，同一周为第一周");
            require(ScheduleRepository.weekNumber(anchor) == 1 && ScheduleRepository.weekNumber(anchor.plusDays(6)) == 1, "周一至周日均为第一周");
            require(ScheduleRepository.weekNumber(anchor.plusWeeks(1)) == 2, "下周一为第二周");
            require(ScheduleRepository.weekNumber(anchor.minusDays(1)) == 0, "上周日不属于第一周");
            require(ScheduleRepository.totalSessionCount() == 20, "选择开学日不改课程数量");
        }
        ScheduleRepository.setSchoolStartDate(LocalDate.of(2027, 1, 1));
        require(ScheduleRepository.weekOneMonday().equals(LocalDate.of(2026, 12, 28)), "跨年第一周正确");
        require(ScheduleRepository.weekNumber(LocalDate.of(2027, 1, 3)) == 1
                && ScheduleRepository.weekNumber(LocalDate.of(2027, 1, 4)) == 2, "跨年周日/周一边界");
        ScheduleRepository.setSchoolStartDate(LocalDate.of(2027, 3, 31));
        require(ScheduleRepository.weekNumber(LocalDate.of(2027, 4, 4)) == 1
                && ScheduleRepository.weekNumber(LocalDate.of(2027, 4, 5)) == 2, "跨月周次正确");
        ScheduleRepository.setImportedRules(List.of());
        require(!ScheduleRepository.hasImportedRules() && ScheduleRepository.totalSessionCount() == 0, "清空后不能退回内置数据");
        ScheduleRepository.setImportedRules(null);
        require(ScheduleRepository.sessionsOn(anchor).isEmpty(), "没有保存数据时保持为空");
        ScheduleRepository.setSchoolStartDate(anchor.plusDays(2));
        require(ScheduleRepository.weekNumber(anchor) == 0, "预设开学日不产生默认课程或周次");
        System.out.println("Schedule validation passed: empty default, imported schedules, statuses, school start dates Monday-Sunday, month/year boundaries, clear-to-empty behavior.");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
