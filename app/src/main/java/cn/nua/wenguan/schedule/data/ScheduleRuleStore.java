package cn.nua.wenguan.schedule.data;

import android.content.Context;
import android.content.SharedPreferences;

import cn.nua.wenguan.schedule.model.ScheduleRule;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ScheduleRuleStore {
    private static final String PREFERENCES = "official_schedule";
    private static final String KEY_RULES = "rules";
    private static final String KEY_SYNC_TIME = "sync_time";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_MONDAY = "week_one_monday";
    private static final String KEY_START_DATE = "school_start_date";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private ScheduleRuleStore() { }

    public static void applySaved(Context context) {
        List<ScheduleRule> rules = load(context);
        ScheduleRepository.setImportedRules(rules, savedStartDate(context));
    }

    public static void save(Context context, List<ScheduleRule> rules, String source, LocalDate startDate) throws JSONException {
        if (rules == null || rules.isEmpty()) throw new JSONException("课表为空，未替换原课表");
        JSONArray array = new JSONArray();
        for (ScheduleRule rule : rules) {
            JSONObject item = new JSONObject();
            item.put("fromWeek", rule.getFromWeek());
            item.put("toWeek", rule.getToWeek());
            item.put("day", rule.getDayOfWeek().name());
            item.put("start", TIME.format(rule.getStartTime()));
            item.put("end", TIME.format(rule.getEndTime()));
            item.put("periods", rule.getPeriods());
            item.put("course", rule.getCourse());
            item.put("room", rule.getRoom());
            item.put("type", rule.getType());
            item.put("teacher", rule.getTeacher());
            array.put(item);
        }
        SharedPreferences prefs = preferences(context);
        Map<String, ?> old = prefs.getAll();
        boolean saved = prefs.edit()
                .putString("previous_rules", prefs.getString(KEY_RULES, ""))
                .putString("previous_monday", prefs.getString(KEY_MONDAY, ""))
                .putString("previous_start_date", prefs.getString(KEY_START_DATE, ""))
                .putString(KEY_RULES, array.toString())
                .putString(KEY_START_DATE, startDate.toString())
                .putString(KEY_MONDAY, ScheduleRepository.weekMonday(startDate).toString())
                .putLong(KEY_SYNC_TIME, System.currentTimeMillis())
                .putString(KEY_SOURCE, source == null ? "南京艺术学院教务系统" : source)
                .commit();
        if (!saved) {
            restorePreferences(prefs, old);
            throw new JSONException("保存课表失败，已回退到原课表");
        }
        ScheduleRepository.setImportedRules(rules, startDate);
    }

    public static LocalDate savedStartDate(Context context) {
        SharedPreferences prefs = preferences(context);
        try {
            // Older versions saved only Monday; retain their week mapping on upgrade.
            String legacy = prefs.getString(KEY_MONDAY, ScheduleRepository.today().toString());
            return LocalDate.parse(prefs.getString(KEY_START_DATE, legacy));
        } catch (RuntimeException ignored) {
            return savedMonday(context);
        }
    }

    public static boolean setStartDate(Context context, LocalDate startDate) {
        SharedPreferences prefs = preferences(context);
        Map<String, ?> old = prefs.getAll();
        if (!prefs.edit().putString(KEY_START_DATE, startDate.toString())
                .putString(KEY_MONDAY, ScheduleRepository.weekMonday(startDate).toString()).commit()) {
            restorePreferences(prefs, old);
            return false;
        }
        ScheduleRepository.setSchoolStartDate(startDate);
        return true;
    }

    public static LocalDate savedMonday(Context context) {
        try {
            return LocalDate.parse(preferences(context).getString(KEY_MONDAY,
                    ScheduleRepository.defaultWeekOneMonday().toString()));
        } catch (RuntimeException ignored) {
            return ScheduleRepository.defaultWeekOneMonday();
        }
    }

    public static boolean clearSchedule(Context context) {
        SharedPreferences prefs = preferences(context);
        Map<String, ?> old = prefs.getAll();
        if (!prefs.edit().remove(KEY_RULES).remove(KEY_MONDAY).remove(KEY_START_DATE)
                .remove(KEY_SYNC_TIME).remove(KEY_SOURCE).remove("previous_rules")
                .remove("previous_monday").remove("previous_start_date").commit()) {
            restorePreferences(prefs, old);
            return false;
        }
        ScheduleRepository.setImportedRules(java.util.Collections.emptyList());
        return true;
    }

    private static void restorePreferences(SharedPreferences prefs, Map<String, ?> old) {
        SharedPreferences.Editor editor = prefs.edit().clear();
        for (Map.Entry<String, ?> entry : old.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) editor.putString(entry.getKey(), (String) value);
            else if (value instanceof Long) editor.putLong(entry.getKey(), (Long) value);
        }
        editor.commit();
    }

    public static long lastSyncTime(Context context) {
        return preferences(context).getLong(KEY_SYNC_TIME, 0L);
    }

    public static String source(Context context) {
        return preferences(context).getString(KEY_SOURCE, "");
    }

    private static List<ScheduleRule> load(Context context) {
        List<ScheduleRule> result = new ArrayList<>();
        String raw = preferences(context).getString(KEY_RULES, "");
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                result.add(new ScheduleRule(
                        item.getInt("fromWeek"),
                        item.getInt("toWeek"),
                        DayOfWeek.valueOf(item.getString("day")),
                        item.getString("start"),
                        item.getString("end"),
                        item.getString("periods"),
                        item.getString("course"),
                        item.optString("room", "地点待定"),
                        item.optString("type", "教务同步"),
                        item.optString("teacher", "")
                ));
            }
        } catch (JSONException | RuntimeException ignored) {
            result.clear();
        }
        return result;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }
}
