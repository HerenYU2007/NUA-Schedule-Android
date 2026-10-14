package cn.nua.wenguan.schedule.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

/** Reads the school's BIFF/OLE export. No formulas, macros or embedded objects are executed. */
public final class ExportedXlsReader {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final byte[] MAGIC = {(byte)0xD0, (byte)0xCF, 0x11, (byte)0xE0,
            (byte)0xA1, (byte)0xB1, 0x1A, (byte)0xE1};
    private static final Pattern GRID_SLOT = Pattern.compile(
            "^([0-9\\s,，、\\-—–~至]+)[（(]\\s*\\[?\\s*([单双]?)周\\s*\\]?\\s*[）)]"
            + "\\s*\\[([0-9\\s,，、\\-—–至]+)节\\]$");
    private ExportedXlsReader() { }

    public static byte[] readBounded(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (Thread.currentThread().isInterrupted()) throw new IOException("读取已取消");
            if (output.size() + count > MAX_BYTES) throw new IOException("文件超过 5 MB，未导入");
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    public static OfficialScheduleParser.ParseResult parse(byte[] data) throws IOException {
        if (data.length > MAX_BYTES) throw new IOException("文件超过 5 MB，未导入");
        if (data.length < MAGIC.length) throw new IOException("文件不是学校导出的二进制 XLS");
        for (int i = 0; i < MAGIC.length; i++) if (data[i] != MAGIC[i])
            throw new IOException("未收到二进制 XLS，可能登录已失效或导出格式不同");
        Workbook workbook = null;
        try {
            WorkbookSettings settings = new WorkbookSettings();
            settings.setSuppressWarnings(true);
            settings.setPropertySets(false);
            settings.setGCDisabled(true);
            workbook = Workbook.getWorkbook(new ByteArrayInputStream(data), settings);
            if (workbook.getNumberOfSheets() > 10) throw new IOException("工作表数量超出支持范围");
            List<String> foundHeaders = null;
            List<List<String>> allRows = new ArrayList<>();
            for (Sheet sheet : workbook.getSheets()) {
                if (sheet.getRows() > 5000 || sheet.getColumns() > 100)
                    throw new IOException("表格尺寸超出支持范围");
                int headerRow = -1;
                List<String> headers = null;
                for (int r = 0; r < Math.min(30, sheet.getRows()); r++) {
                    List<String> row = row(sheet, r);
                    if (has(row, "课程名称", "课程名") && has(row, "实际上课时间", "上课时间")) {
                        headerRow = r; headers = row; break;
                    }
                }
                if (headerRow < 0) {
                    OfficialScheduleParser.ParseResult grid = parseGrid(sheet);
                    if (grid != null) {
                        if (foundHeaders != null || workbook.getNumberOfSheets() != 1)
                            throw new IOException("文件包含多个工作表，请分别导出后导入");
                        return grid;
                    }
                    continue;
                }
                // Do not silently merge unlike worksheets or choose one of several semesters.
                if (foundHeaders != null) throw new IOException("文件包含多个课表，请分别导出后导入");
                foundHeaders = headers;
                for (int r = headerRow + 1; r < sheet.getRows(); r++) allRows.add(row(sheet, r));
            }
            if (foundHeaders == null) throw new IOException("XLS 中未找到课程名称和上课时间列");
            try { return OfficialScheduleParser.parseRows(foundHeaders, allRows); }
            catch (RuntimeException error) { throw new IOException("课表内容未完整识别，请核对导出文件", error); }
        } catch (BiffException | RuntimeException error) {
            throw new IOException("XLS 格式损坏、加密或不受支持", error);
        } finally {
            if (workbook != null) workbook.close();
        }
    }

    private static OfficialScheduleParser.ParseResult parseGrid(Sheet sheet) throws IOException {
        int header = -1;
        int[] days = new int[sheet.getColumns()];
        for (int r = 0; r < Math.min(30, sheet.getRows()); r++) {
            Set<Integer> unique = new LinkedHashSet<>();
            for (int c = 0; c < sheet.getColumns(); c++) {
                String label = sheet.getCell(c, r).getContents().replaceAll("\\s+", "");
                days[c] = label.matches("(?:星期|周)[一二三四五六日天]")
                        ? label.endsWith("天") ? 7 : "一二三四五六日".indexOf(label.charAt(label.length() - 1)) + 1 : 0;
                if (days[c] > 0) unique.add(days[c]);
            }
            if (unique.size() == 7) { header = r; break; }
        }
        if (header < 0) return null;
        List<List<String>> rows = new ArrayList<>();
        Set<String> warnings = new LinkedHashSet<>();
        int excluded = 0;
        for (int r = header + 1; r < sheet.getRows(); r++) for (int c = 0; c < sheet.getColumns(); c++) {
            String value = sheet.getCell(c, r).getContents();
            if (value.isBlank()) continue;
            if (value.contains("军事技能")) { excluded++; continue; }
            if (days[c] == 0) continue; // Time labels are not the course's actual periods.
            parseGridCell(value, days[c], rows, warnings);
        }
        OfficialScheduleParser.ParseResult parsed;
        try { parsed = OfficialScheduleParser.parseRows(
                List.of("课程名称", "实际上课时间", "实际上课教室", "授课教师"), rows); }
        catch (RuntimeException error) { throw new IOException("未能识别导出周视图的课程信息", error); }
        warnings.addAll(parsed.getWarnings());
        warnings.add("导出周视图未单独提供理论/实践周次；已使用文件中的周次、节次和教室，请核对学期。 ");
        return new OfficialScheduleParser.ParseResult(new ArrayList<>(parsed.getRules()),
                new LinkedHashSet<>(parsed.getCourses()), new ArrayList<>(warnings), excluded + parsed.getSkippedCount());
    }

    // Public for synthetic, identity-free regression fixtures as well as BIFF cells.
    public static void parseGridCell(String value, int day, List<List<String>> rows, Set<String> warnings)
            throws IOException {
        List<String> lines = new ArrayList<>();
        for (String line : value.replace('\u00a0', ' ').split("\\r?\\n"))
            if (!line.trim().isEmpty() && !line.trim().matches("[-—─]{3,}")) lines.add(line.trim());
        int start = 0;
        String previousCourse = null, previousTeacher = null;
        boolean recognized = false;
        for (int i = 0; i < lines.size(); i++) {
            Matcher marker = GRID_SLOT.matcher(lines.get(i));
            if (!marker.matches()) continue;
            recognized = true;
            List<String> fields = new ArrayList<>(lines.subList(start, i));
            int college = -1;
            for (int f = 0; f < fields.size(); f++) if (fields.get(f).endsWith("学院")
                    || fields.get(f).equals("体育部")) college = f;
            if (college >= 0) fields = new ArrayList<>(fields.subList(college + 1, fields.size()));
            String course, teacher;
            if (fields.isEmpty() && previousCourse != null) { course = previousCourse; teacher = previousTeacher; }
            else {
                if (fields.size() < 2) throw new IOException("课程与教师分行结构无法确认，未导入");
                teacher = fields.remove(fields.size() - 1);
                fields.removeIf(f -> f.matches("[（(].*(?:\\d+\\s*班|混合班|教学班)[）)]"));
                course = String.join("", fields);
                if (course.isEmpty() || teacher.matches(".*(?:\\d+周|\\d+节|\\[|\\]).*"))
                    throw new IOException("课程与教师结构无法确认，未导入");
            }
            if (i + 1 >= lines.size() || GRID_SLOT.matcher(lines.get(i + 1)).matches())
                throw new IOException("课程地点缺失，未导入");
            String room = lines.get(++i);
            start = i + 1;
            previousCourse = course; previousTeacher = teacher;
            Set<Integer> periods = new java.util.TreeSet<>();
            String text = marker.group(3).replaceAll("\\s+", "");
            try {
                if (text.contains("至")) {
                    String[] range = text.split("至", -1);
                    if (range.length != 2) throw new NumberFormatException();
                    int first = Integer.parseInt(range[0]), last = Integer.parseInt(range[1]);
                    if (first < 1 || last > 12 || first > last) throw new NumberFormatException();
                    for (int p = first; p <= last; p++) periods.add(p);
                } else for (String number : text.split("[-—–,，、]", -1)) {
                    int p = Integer.parseInt(number);
                    if (p < 1 || p > 12) throw new NumberFormatException();
                    periods.add(p);
                }
            } catch (NumberFormatException error) { throw new IOException("导出文件节次超出支持范围"); }
            if (periods.isEmpty() || day < 1 || day > 7) throw new IOException("节次或星期无法确认");
            List<String> times = new ArrayList<>();
            int first = -1, last = -1;
            for (int p : periods) {
                if (first < 0) first = last = p;
                else if (p == last + 1) last = p;
                else { times.add(time(marker, day, first, last)); first = last = p; }
            }
            times.add(time(marker, day, first, last));
            if (times.size() > 1) warnings.add("有不连续节次，已按文件列出的节次分别导入，请核对。");
            rows.add(List.of(course, String.join(";", times), room, teacher));
        }
        if (!recognized || start != lines.size()) throw new IOException("部分课程块无法完整识别，未导入");
    }

    private static String time(Matcher marker, int day, int first, int last) {
        return marker.group(1).replaceAll("\\s+", "") + "周" + (marker.group(2).isEmpty()
                ? "" : "(" + marker.group(2) + ")") + ",周" + "一二三四五六日".charAt(day - 1)
                + ",第" + first + (first == last ? "" : "-" + last) + "节";
    }

    private static List<String> row(Sheet sheet, int r) {
        List<String> values = new ArrayList<>();
        for (int c = 0; c < sheet.getColumns(); c++) values.add(sheet.getCell(c, r).getContents());
        return values;
    }

    private static boolean has(List<String> row, String... aliases) {
        for (String cell : row) for (String alias : aliases)
            if (cell.replaceAll("\\s+", "").equals(alias)) return true;
        return false;
    }
}
