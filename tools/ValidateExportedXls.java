import cn.nua.wenguan.schedule.data.ExportedXlsReader;
import cn.nua.wenguan.schedule.data.OfficialScheduleParser;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;

public class ValidateExportedXls {
    public static void main(String[] args) throws Exception {
        ArrayList<List<String>> rows = new ArrayList<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        ExportedXlsReader.parseGridCell("测试课程\n(测试课程 1班)\n周老师\n6-8([周])[02-03-04节]\n文101\n"
                + "测试课程\n(测试课程 1班)\n周老师\n9([周])[03-04节]\n校外课堂", 2, rows, warnings);
        OfficialScheduleParser.ParseResult fixture = OfficialScheduleParser.parseRows(
                List.of("课程名称", "实际上课时间", "实际上课教室", "授课教师"), rows);
        if (fixture.getCourseCount() != 1 || fixture.getRules().size() != 4)
            throw new AssertionError("Grid must read multiple blocks and remove class annotations");
        if (!fixture.getRules().get(0).getStartTime().equals(java.time.LocalTime.of(8, 50)))
            throw new AssertionError("Use explicit period 2, not the enclosing row's period 1");
        if (!fixture.getRules().get(3).getRoom().equals("校外课堂"))
            throw new AssertionError("Segmented room must be preserved");
        rows.clear();
        ExportedXlsReader.parseGridCell("测试课程\n老师\n1-4(单周)[01-03节]\n文101", 1, rows, warnings);
        fixture = OfficialScheduleParser.parseRows(List.of("课程名称", "实际上课时间", "教室", "教师"), rows);
        if (fixture.getRules().size() != 4 || warnings.isEmpty()) throw new AssertionError("Explicit gaps and parity");
        try {
            ExportedXlsReader.parseGridCell("测试课程\n老师\n1([周])[01-02节]", 1, rows, warnings);
            throw new AssertionError("Missing room must fail");
        } catch (IOException expected) { }
        try {
            ExportedXlsReader.parseGridCell("未知课程结构", 1, rows, warnings);
            throw new AssertionError("Unknown blocks must not be silently omitted");
        } catch (IOException expected) { }
        try {
            ExportedXlsReader.parse("<html>login</html>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            throw new AssertionError("HTML cannot masquerade as XLS");
        } catch (IOException expected) { }
        try {
            ExportedXlsReader.readBounded(new ByteArrayInputStream(new byte[ExportedXlsReader.MAX_BYTES + 1]));
            throw new AssertionError("Oversized import must fail");
        } catch (IOException expected) { }
        if (args.length == 1) {
            try (FileInputStream input = new FileInputStream(args[0])) {
                OfficialScheduleParser.ParseResult result = ExportedXlsReader.parse(ExportedXlsReader.readBounded(input));
                if (result.getCourses().stream().anyMatch(c -> c.contains("军事技能")))
                    throw new AssertionError("Military skills must remain excluded");
                System.out.println("School XLS: " + result.getCourseCount() + " courses, " + result.getRules().size()
                        + " meetings, " + result.getSkippedCount() + " excluded rows, " + result.getWarnings().size() + " warnings");
            }
        }
        System.out.println("XLS format and size guards passed");
    }
}
