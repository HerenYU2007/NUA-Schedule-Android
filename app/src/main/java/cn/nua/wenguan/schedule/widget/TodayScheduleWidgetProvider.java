package cn.nua.wenguan.schedule.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.SizeF;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import cn.nua.wenguan.schedule.MainActivity;
import cn.nua.wenguan.schedule.R;
import cn.nua.wenguan.schedule.data.ScheduleRepository;
import cn.nua.wenguan.schedule.data.ScheduleRuleStore;
import cn.nua.wenguan.schedule.model.CourseSession;
import cn.nua.wenguan.schedule.model.CourseStatus;
import cn.nua.wenguan.schedule.ui.ScheduleText;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class TodayScheduleWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "cn.nua.wenguan.schedule.widget.REFRESH";
    public static final String ACTION_SHOW_TODAY = "cn.nua.wenguan.schedule.widget.SHOW_TODAY";
    public static final String ACTION_SHOW_TOMORROW = "cn.nua.wenguan.schedule.widget.SHOW_TOMORROW";
    public static final String ACTION_AUTO_TOMORROW = "cn.nua.wenguan.schedule.widget.AUTO_TOMORROW";
    public static final String ACTION_RESET_TODAY = "cn.nua.wenguan.schedule.widget.RESET_TODAY";
    private static final String PREFERENCES = "widget_preferences";
    private static final String KEY_DAY_OFFSET = "day_offset";

    private static final int[] COURSE_ROWS = {
            R.id.widget_course_row_1,
            R.id.widget_course_row_2,
            R.id.widget_course_row_3
    };
    private static final int[] COURSE_TIMES = {
            R.id.widget_course_time_1,
            R.id.widget_course_time_2,
            R.id.widget_course_time_3
    };
    private static final int[] COURSE_NAMES = {
            R.id.widget_course_name_1,
            R.id.widget_course_name_2,
            R.id.widget_course_name_3
    };
    private static final int[] COURSE_DETAILS = {
            R.id.widget_course_detail_1,
            R.id.widget_course_detail_2,
            R.id.widget_course_detail_3
    };
    private static final int[] COURSE_STATUSES = {
            R.id.widget_course_status_1,
            R.id.widget_course_status_2,
            R.id.widget_course_status_3
    };

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        setDayOffset(context, ScheduleRepository.now().getHour() >= 22 ? 1 : 0);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        ensureInitialDay(context);
        for (int appWidgetId : appWidgetIds) {
            manager.updateAppWidget(appWidgetId, buildSizedViews(context, manager.getAppWidgetOptions(appWidgetId)));
        }
        scheduleNextRefresh(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle options) {
        ensureInitialDay(context);
        manager.updateAppWidget(id, buildSizedViews(context, options));
        scheduleNextRefresh(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_SHOW_TODAY.equals(action)) {
            setDayOffset(context, 0);
            updateAll(context);
        } else if (ACTION_SHOW_TOMORROW.equals(action) || ACTION_AUTO_TOMORROW.equals(action)) {
            setDayOffset(context, 1);
            updateAll(context);
        } else if (ACTION_RESET_TODAY.equals(action) || Intent.ACTION_DATE_CHANGED.equals(action)) {
            setDayOffset(context, 0);
            updateAll(context);
        } else if (Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            setDayOffset(context, ScheduleRepository.now().getHour() >= 22 ? 1 : 0);
            updateAll(context);
        } else if (ACTION_REFRESH.equals(action)) {
            ensureInitialDay(context);
            updateAll(context);
        }
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        WidgetUpdateScheduler.cancel(context);
    }

    private static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, TodayScheduleWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(provider);
        for (int id : ids) {
            manager.updateAppWidget(id, buildSizedViews(context, manager.getAppWidgetOptions(id)));
        }
        if (ids.length > 0) {
            scheduleNextRefresh(context);
        }
    }

    private static RemoteViews buildSizedViews(Context context, Bundle options) {
        if (options == null) options = new Bundle();
        int rawMinWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        int rawMaxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0);
        int rawMinHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
        int rawMaxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
        if (Build.VERSION.SDK_INT >= 31) {
            ArrayList<SizeF> sizes = options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
            Map<SizeF, RemoteViews> exactViews = new LinkedHashMap<>();
            if (sizes != null) for (SizeF size : sizes) {
                if (size == null || !WidgetSizePolicy.current(size.getWidth(), size.getHeight(),
                        rawMinWidth, rawMaxWidth, rawMinHeight, rawMaxHeight)) continue;
                exactViews.put(size, buildViews(context, size.getWidth(), size.getHeight()));
                if (exactViews.size() >= 16) break;
            }
            if (!exactViews.isEmpty()) return new RemoteViews(exactViews);
        }
        // Older/OEM hosts may omit the exact sizes. Supply both orientations as a fallback.
        int[] widths = WidgetSizePolicy.range(rawMinWidth, rawMaxWidth, 250);
        int[] heights = WidgetSizePolicy.range(rawMinHeight, rawMaxHeight, 120);
        int minWidth = widths[0], maxWidth = widths[1];
        int minHeight = heights[0], maxHeight = heights[1];
        return new RemoteViews(buildViews(context, maxWidth, minHeight), buildViews(context, minWidth, maxHeight));
    }

    private static RemoteViews buildViews(Context context, float width, float height) {
        ScheduleRuleStore.applySaved(context);
        LocalDate today = ScheduleRepository.today();
        int dayOffset = getDayOffset(context);
        LocalDate shownDate = today.plusDays(dayOffset);
        int week = ScheduleRepository.weekNumber(shownDate);
        List<CourseSession> allSessions = ScheduleRepository.sessionsOn(shownDate);
        ZonedDateTime now = ScheduleRepository.now();
        List<CourseSession> visibleSessions = new ArrayList<>();
        for (CourseSession session : allSessions) {
            if (ScheduleRepository.statusAt(session, now) != CourseStatus.ENDED) {
                visibleSessions.add(session);
            }
        }

        boolean tablet = context.getResources().getConfiguration().smallestScreenWidthDp >= 600;
        WidgetLayoutPolicy layout = new WidgetLayoutPolicy(width, height, context.getResources().getConfiguration().fontScale, tablet);
        int rowLimit = layout.rowsForCount(visibleSessions.size());
        RemoteViews views = new RemoteViews(context.getPackageName(), layout.phoneLayout ? R.layout.widget_phone : R.layout.widget_today);
        applySizing(context, views, layout);
        views.setViewVisibility(R.id.widget_summary, layout.compact && !visibleSessions.isEmpty() ? View.GONE : View.VISIBLE);
        String dayLabel = dayOffset == 0 ? "今天" : "明天";
        views.setTextViewText(R.id.widget_date, dayLabel + " · " + (layout.phoneLayout
                ? ScheduleText.shortDateTitle(shownDate, 0) : layout.compact
                ? shownDate.getMonthValue() + "/" + shownDate.getDayOfMonth()
                : ScheduleText.shortDateTitle(shownDate, week)));
        if (!ScheduleRepository.hasImportedRules()) {
            views.setTextViewText(R.id.widget_summary, "尚未导入课表");
        } else if (allSessions.isEmpty()) {
            views.setTextViewText(R.id.widget_summary, dayLabel + "没有课");
        } else if (visibleSessions.isEmpty()) {
            views.setTextViewText(R.id.widget_summary, "今日课程已结束");
        } else if (dayOffset == 1) {
            views.setTextViewText(R.id.widget_summary, "明天共 " + visibleSessions.size() + " 节课");
        } else {
            views.setTextViewText(R.id.widget_summary, "剩余 " + visibleSessions.size() + " 节课");
        }

        if (dayOffset == 0 && !visibleSessions.isEmpty()) {
            CourseSession next = ScheduleRepository.nextSession(shownDate, now);
            if (next != null) {
                views.setTextViewText(R.id.widget_summary, ScheduleRepository.countdownText(next, now));
            }
        }

        if (layout.phoneLayout && week > 0) {
            String count = visibleSessions.isEmpty() ? (allSessions.isEmpty() ? dayLabel + "没有课" : "今日课程已结束")
                    : (dayOffset == 1 ? "明天共 " : "剩余 ") + visibleSessions.size() + " 节课";
            CourseSession next = dayOffset == 0 ? ScheduleRepository.nextSession(shownDate, now) : null;
            views.setTextViewText(R.id.widget_summary, "第" + week + "周 · " + (next == null ? count : ScheduleRepository.countdownText(next, now)));
        }

        for (int i = 0; i < COURSE_ROWS.length; i++) {
            if (i < visibleSessions.size() && i < rowLimit) {
                CourseSession session = visibleSessions.get(i);
                CourseStatus status = ScheduleRepository.statusAt(session, now);
                views.setViewVisibility(COURSE_ROWS[i], View.VISIBLE);
                views.setTextViewText(COURSE_TIMES[i], session.getTimeRange());
                views.setTextViewText(COURSE_NAMES[i], session.getCourse());
                views.setTextViewText(
                        COURSE_DETAILS[i],
                        session.getRoom() + (layout.compact ? "" : " · " + session.getType() + " · " + session.getPeriods())
                );
                views.setTextViewText(COURSE_STATUSES[i], status.getLabel());
                views.setTextColor(COURSE_STATUSES[i], widgetStatusColor(status));
            } else {
                views.setViewVisibility(COURSE_ROWS[i], View.GONE);
            }
        }

        int overflow = Math.max(0, visibleSessions.size() - rowLimit);
        views.setViewVisibility(R.id.widget_more, overflow > 0 ? View.VISIBLE : View.GONE);
        views.setTextViewText(R.id.widget_more, (rowLimit == 0 ? "当日 " : "还有 ") + overflow + " 节 · 拉高或点击查看");
        if (layout.phoneLayout) views.setViewVisibility(R.id.widget_courses, visibleSessions.isEmpty() ? View.GONE : View.VISIBLE);

        boolean showEmpty = visibleSessions.isEmpty();
        views.setViewVisibility(R.id.widget_empty, showEmpty ? View.VISIBLE : View.GONE);
        if (!ScheduleRepository.hasImportedRules()) {
            views.setTextViewText(R.id.widget_empty, "课表为空\n点击打开 App，登录教务系统导入");
        } else if (!ScheduleRepository.isVisibleWeek(week)) {
            views.setTextViewText(R.id.widget_empty, "当前不在" + ScheduleRepository.rangeText() + "课表范围内");
        } else if (!allSessions.isEmpty()) {
            views.setTextViewText(R.id.widget_empty, "今天的课程已全部结束");
        } else {
            views.setTextViewText(R.id.widget_empty, dayLabel + "没有课");
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, openPendingIntent);

        Intent refreshIntent = new Intent(context, TodayScheduleWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                context, 1, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_refresh, refreshPendingIntent);

        Intent todayIntent = new Intent(context, TodayScheduleWidgetProvider.class);
        todayIntent.setAction(ACTION_SHOW_TODAY);
        PendingIntent todayPendingIntent = PendingIntent.getBroadcast(
                context, 2, todayIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_previous, todayPendingIntent);

        Intent tomorrowIntent = new Intent(context, TodayScheduleWidgetProvider.class);
        tomorrowIntent.setAction(ACTION_SHOW_TOMORROW);
        PendingIntent tomorrowPendingIntent = PendingIntent.getBroadcast(
                context, 3, tomorrowIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_next, tomorrowPendingIntent);
        return views;
    }

    private static void applySizing(Context context, RemoteViews views, WidgetLayoutPolicy layout) {
        float density = context.getResources().getDisplayMetrics().density;
        int padding = Math.round(layout.padding * density);
        views.setViewPadding(R.id.widget_root, padding, padding, padding, padding);
        views.setTextViewTextSize(R.id.widget_date, TypedValue.COMPLEX_UNIT_PX, layout.headerSize * density);
        views.setTextViewTextSize(R.id.widget_summary, TypedValue.COMPLEX_UNIT_PX, layout.footerSize * density);
        views.setTextViewTextSize(R.id.widget_more, TypedValue.COMPLEX_UNIT_PX, layout.footerSize * density);
        views.setTextViewTextSize(R.id.widget_empty, TypedValue.COMPLEX_UNIT_PX, layout.nameSize * density);
        for (int id : new int[] {R.id.widget_previous, R.id.widget_next, R.id.widget_refresh}) {
            float size = layout.phoneLayout ? (id == R.id.widget_refresh ? 14 : 12) : layout.footerSize;
            views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_PX, size * density);
        }
        for (int i = 0; i < COURSE_ROWS.length; i++) {
            views.setTextViewTextSize(COURSE_TIMES[i], TypedValue.COMPLEX_UNIT_PX, layout.timeSize * density);
            views.setTextViewTextSize(COURSE_NAMES[i], TypedValue.COMPLEX_UNIT_PX, layout.nameSize * density);
            views.setTextViewTextSize(COURSE_DETAILS[i], TypedValue.COMPLEX_UNIT_PX, layout.detailSize * density);
            views.setTextViewTextSize(COURSE_STATUSES[i], TypedValue.COMPLEX_UNIT_PX, layout.statusSize * density);
        }
    }

    private static void scheduleNextRefresh(Context context) {
        LocalDate today = ScheduleRepository.today();
        WidgetUpdateScheduler.scheduleNext(
                context,
                ScheduleRepository.sessionsOn(today),
                ScheduleRepository.now()
        );
    }

    private static int getDayOffset(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getInt(KEY_DAY_OFFSET, 0);
    }

    private static void setDayOffset(Context context, int offset) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_DAY_OFFSET, offset == 1 ? 1 : 0)
                .apply();
    }

    private static void ensureInitialDay(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        if (!preferences.contains(KEY_DAY_OFFSET)) {
            int offset = ScheduleRepository.now().getHour() >= 22 ? 1 : 0;
            preferences.edit().putInt(KEY_DAY_OFFSET, offset).apply();
        }
    }

    private static int widgetStatusColor(CourseStatus status) {
        switch (status) {
            case IN_PROGRESS: return 0xFF78E6B6;
            case ENDED: return 0xFFB8C3CA;
            case UPCOMING:
            default: return 0xFF9ED6FF;
        }
    }
}
