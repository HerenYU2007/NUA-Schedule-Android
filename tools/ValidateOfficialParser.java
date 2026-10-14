import cn.nua.wenguan.schedule.data.OfficialScheduleParser;
import cn.nua.wenguan.schedule.data.ScheduleRepository;
import cn.nua.wenguan.schedule.model.ScheduleRule;
import java.time.LocalDate;
import java.util.List;

public final class ValidateOfficialParser {
    private static final List<String> HEADERS = List.of("课程名称", "实际上课时间", "实际上课教室", "授课教师", "理论周次", "实践周次");
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static OfficialScheduleParser.ParseResult parse(String time, String room) {
        return OfficialScheduleParser.parseRows(HEADERS, List.of(List.of("测试课程", time, room, "教师", "", "")));
    }
    public static void main(String[] args) {
        var management = OfficialScheduleParser.parseRows(HEADERS, List.of(List.of("管理学原理",
                "5-12周,周五,第2-4节;13周,周五,第2-2节;13周,周五,第3-4节;14周,周五,第3-4节;15周,周五,第2-4节;",
                "文313;文313;校外课堂;校外课堂;文313", "周瓅", "13,15,5-12", "13,14")));
        require(management.getRules().size() == 12, "分段课程必须保留全部12次上课");
        require(management.getRules().get(8).getRoom().equals("文313"), "13周第2节应在文313");
        require(management.getRules().get(8).getType().equals("理论"), "同周同时含理论与实践时应按对应地点区分");
        require(management.getRules().get(9).getRoom().equals("校外课堂"), "13周第3—4节应为校外课堂");
        require(management.getRules().get(11).getRoom().equals("文313"), "15周恢复原教室");
        require(management.getRules().get(9).getType().equals("实践"), "实践周次识别");
        var parity = parse("第1-16周(单),星期二(01-02节);", "文210");
        require(parity.getRules().size() == 8, "单周应只保留8次");
        require(parity.getRules().get(7).getToWeek() == 15, "末次单周为15周");
        var even = parse("2,4,6周双，周3，第5至7节", "实703");
        require(even.getRules().size() == 3, "周次列表、数字星期识别");
        require(even.getRules().get(0).getEndTime().toString().equals("16:50"), "跨三节时间须与内置推算一致");
        var rooms = parse("1-3周,周一,第1-2节", "A\nB\nC");
        require(rooms.getRules().get(1).getRoom().equals("B"), "按周教室逐项对应");
        var mismatch = parse("1-4周,周一,第1-2节", "A;B");
        require(mismatch.getWarnings().size() == 1, "教室错位必须预警");
        require(mismatch.getRules().get(0).getRoom().equals("地点待核对"), "不猜测错位教室");
        var partial = OfficialScheduleParser.parseRows(HEADERS, List.of(
                List.of("正常课程", "1周,周一,第1-2节", "A", "教师", "", ""),
                List.of("军事技能", "1周,周二,第1-2节", "A", "教师", "", ""),
                List.of("未支持课程", "1周,周三,第1-2节;未知时间", "A", "教师", "", "")));
        require(partial.getCourseCount() == 1 && partial.getSkippedCount() == 1, "军事技能排除");
        require(partial.getWarnings().size() == 1, "部分无法识别的课程不能默默导入");
        boolean failed = false;
        try { parse("1周,周一,第1-2节;2周,周一,第13-14节", "A"); }
        catch (IllegalArgumentException expected) { failed = true; }
        require(failed, "非法节次不可默默丢弃");
        var duplicates = OfficialScheduleParser.parseRows(HEADERS, List.of(
                List.of("同一课程", "1周,周一,第1-2节", "A", "教师", "", ""),
                List.of("同一课程", "1周,周一,第1-2节", "A", "教师", "", "")));
        require(duplicates.getRules().size() == 1, "重复行去重");
        var full = parse("1-20周,周一,第1-2节", "文210");
        ScheduleRepository.setImportedRules(full.getRules(), LocalDate.of(2027, 2, 24));
        require(ScheduleRepository.weekOneMonday().equals(LocalDate.of(2027, 2, 22)), "日期自动规范为周一");
        require(ScheduleRepository.sessionsInWeek(1).size() == 1, "导入课表支持第1周");
        require(ScheduleRepository.sessionsInWeek(20).size() == 1, "导入课表支持第20周");
        require(ScheduleRepository.weekNumber(LocalDate.of(2027, 2, 22)) == 1, "新学期周次锚点");
        require(ScheduleRepository.totalSessionCount() == 20, "导入课次计数");
        ScheduleRepository.setImportedRules(List.of());
        require(ScheduleRepository.totalSessionCount() == 0, "清空后恢复为空课表");
        System.out.println("Official parser validation passed: segmented rooms, parity, exclusion, warnings, dates, deduplication.");
    }
}
