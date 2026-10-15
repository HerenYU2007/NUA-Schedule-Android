package cn.nua.wenguan.schedule.data;

import cn.nua.wenguan.schedule.model.ScheduleRule;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OfficialScheduleParser {
    private static final String NUMBERS = "[0-9\\s,，、\\-—–~至]+";
    private static final Pattern SCHEDULE_PATTERN = Pattern.compile(
            "第?\\s*(" + NUMBERS + ")\\s*周\\s*[(（]?\\s*([单双])?(?:周)?\\s*[,，;；()（）\\s]*"
            + "(?:星期|周)\\s*([一二三四五六日天1-7])\\s*[,，;；(（\\s]*"
            + "第?\\s*(\\d+)\\s*(?:[-—–~至]\\s*(\\d+))?\\s*节");

    private OfficialScheduleParser() { }

    public static ParseResult parse(String json) throws JSONException {
        JSONObject object = new JSONObject(json);
        List<String> headers = strings(object.getJSONArray("headers"));
        List<List<String>> rows = new ArrayList<>();
        JSONArray array = object.getJSONArray("rows");
        for (int i = 0; i < array.length(); i++) rows.add(strings(array.getJSONArray(i)));
        return parseRows(headers, rows);
    }

    // Independent of Android so real school table formats can be regression-tested.
    public static ParseResult parseRows(List<String> headers, List<List<String>> rows) {
        int courseColumn = column(headers, "课程名称", "课程名");
        int timeColumn = column(headers, "实际上课时间", "上课时间");
        int roomColumn = column(headers, "实际上课教室", "上课地点", "上课教室", "教室");
        int teacherColumn = column(headers, "授课教师", "上课教师", "教师");
        int theoryColumn = column(headers, "理论周次");
        int practiceColumn = column(headers, "实践周次");
        if (courseColumn < 0 || timeColumn < 0) {
            throw new IllegalArgumentException("当前页面没有课程名称和上课时间列，请打开个人课表的列表视图。");
        }
        List<ScheduleRule> rules = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> courses = new LinkedHashSet<>();
        Set<String> dedup = new LinkedHashSet<>();
        int skipped = 0;
        for (List<String> row : rows) {
            String course = clean(cell(row, courseColumn));
            if (course.isEmpty() || course.equals("课程名称")) continue;
            if (course.contains("军事技能")) { skipped++; continue; }
            String schedule = clean(cell(row, timeColumn));
            if (schedule.isEmpty()) {
                warnings.add(course + "：没有上课时间，未导入。");
                continue;
            }
            List<Slot> slots = slots(schedule);
            if (slots.isEmpty()) {
                warnings.add(course + "：未识别周次或节次，未导入。");
                continue;
            }
            String residual = SCHEDULE_PATTERN.matcher(schedule).replaceAll("")
                    .replaceAll("[\\s;；,，()（）单双周]+", "");
            if (!residual.isEmpty()) {
                warnings.add(course + "：部分上课时间未识别，整行未导入。");
                continue;
            }
            List<String> rooms = splitValues(cell(row, roomColumn));
            List<String> teachers = splitValues(cell(row, teacherColumn));
            boolean roomAligned = rooms.size() <= 1 || rooms.size() == slots.size();
            int meetings = 0;
            for (Slot slot : slots) meetings += slot.weeks.size();
            boolean roomsPerWeek = !roomAligned && rooms.size() == meetings;
            if (!roomAligned && !roomsPerWeek) {
                warnings.add(course + "：教室数量与时间段不对应，显示“地点待核对”。");
            }
            Set<Integer> theory = weeksOrEmpty(cell(row, theoryColumn));
            Set<Integer> practice = weeksOrEmpty(cell(row, practiceColumn));
            int occurrence = 0;
            for (int i = 0; i < slots.size(); i++) {
                Slot slot = slots.get(i);
                String teacher = teachers.isEmpty() ? "" : teachers.size() == slots.size()
                        ? teachers.get(i) : String.join("、", teachers);
                for (int week : slot.weeks) {
                    String room = roomsPerWeek ? rooms.get(occurrence) : rooms.isEmpty()
                            ? "地点待定" : rooms.size() == 1 ? rooms.get(0) : roomAligned
                            ? rooms.get(i) : "地点待核对";
                    occurrence++;
                    boolean practiceRoom = room.contains("校外") || room.contains("虚拟") || course.contains("体育");
                    // A week may contain a theory session and a separate practice session.
                    String type = practice.contains(week) && theory.contains(week)
                            ? practiceRoom ? "实践" : "理论"
                            : practice.contains(week) ? "实践" : theory.contains(week) ? "理论"
                            : practiceRoom ? "实践" : "教务同步";
                    String key = week + "|" + slot.day + "|" + slot.first + "|" + slot.last
                            + "|" + course + "|" + room + "|" + teacher;
                    if (dedup.add(key)) {
                        String periods = slot.first == slot.last ? "第" + slot.first + "节"
                                : "第" + slot.first + "–" + slot.last + "节";
                        rules.add(new ScheduleRule(week, week, slot.day, SchoolPeriodTimes.start(slot.first), SchoolPeriodTimes.end(slot.last),
                                periods, course, room, type, teacher));
                        courses.add(course);
                    }
                }
            }
        }
        if (rules.isEmpty()) throw new IllegalArgumentException(
                "未识别出可导入的课程。请打开学生个人课表列表，确认学期和完整课表后重试。");
        return new ParseResult(rules, courses, warnings, skipped);
    }

    private static List<Slot> slots(String schedule) {
        List<Slot> result = new ArrayList<>();
        Matcher matcher = SCHEDULE_PATTERN.matcher(schedule);
        while (matcher.find()) {
            Set<Integer> weeks = weekNumbers(matcher.group(1));
            String parity = matcher.group(2);
            if (parity != null) weeks.removeIf(week -> "单".equals(parity) ? week % 2 == 0 : week % 2 != 0);
            int first = Integer.parseInt(matcher.group(4));
            int last = matcher.group(5) == null ? first : Integer.parseInt(matcher.group(5));
            if (weeks.isEmpty() || first < 1 || last > 12 || last < first) {
                throw new IllegalArgumentException("课表包含无法支持的周次或节次，请先核对完整课表。");
            }
            int day = "一二三四五六日".indexOf(matcher.group(3)) + 1;
            if (day == 0) day = "天".equals(matcher.group(3)) ? 7 : Integer.parseInt(matcher.group(3));
            result.add(new Slot(weeks, DayOfWeek.of(day), first, last));
        }
        return result;
    }

    private static Set<Integer> weekNumbers(String raw) {
        Set<Integer> result = new LinkedHashSet<>();
        for (String part : raw.trim().split("[,，、]")) {
            String[] range = part.trim().split("[-—–~至]");
            if (range.length == 0 || range[0].trim().isEmpty()) continue;
            int first = Integer.parseInt(range[0].trim());
            int last = range.length == 1 ? first : Integer.parseInt(range[1].trim());
            if (first < 1 || last > 53 || last < first) throw new IllegalArgumentException("课表周次超出范围");
            for (int week = first; week <= last; week++) result.add(week);
        }
        return result;
    }

    private static Set<Integer> weeksOrEmpty(String raw) {
        try { return weekNumbers(raw.replace("周", "").replace("第", "")); }
        catch (RuntimeException ignored) { return Collections.emptySet(); }
    }

    private static List<String> splitValues(String text) {
        List<String> result = new ArrayList<>();
        for (String part : text.split("[;；:：\\r\\n]+")) {
            String value = clean(part);
            if (!value.isEmpty()) result.add(value);
        }
        return result;
    }

    private static int column(List<String> headers, String... aliases) {
        for (String alias : aliases) {
            for (int i = 0; i < headers.size(); i++) if (clean(headers.get(i)).equals(alias)) return i;
        }
        return -1;
    }

    private static List<String> strings(JSONArray array) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) result.add(array.optString(i, ""));
        return result;
    }

    private static String cell(List<String> row, int index) {
        return index >= 0 && index < row.size() ? row.get(index) : "";
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }

    private static final class Slot {
        final Set<Integer> weeks;
        final DayOfWeek day;
        final int first;
        final int last;
        Slot(Set<Integer> weeks, DayOfWeek day, int first, int last) {
            this.weeks = weeks; this.day = day; this.first = first; this.last = last;
        }
    }

    public static final class ParseResult {
        private final List<ScheduleRule> rules;
        private final Set<String> courses;
        private final List<String> warnings;
        private final int skipped;
        ParseResult(List<ScheduleRule> rules, Set<String> courses, List<String> warnings, int skipped) {
            this.rules = Collections.unmodifiableList(rules);
            this.courses = Collections.unmodifiableSet(courses);
            this.warnings = Collections.unmodifiableList(warnings);
            this.skipped = skipped;
        }
        public List<ScheduleRule> getRules() { return rules; }
        public int getCourseCount() { return courses.size(); }
        public Set<String> getCourses() { return courses; }
        public List<String> getWarnings() { return warnings; }
        public int getSkippedCount() { return skipped; }
    }
}
