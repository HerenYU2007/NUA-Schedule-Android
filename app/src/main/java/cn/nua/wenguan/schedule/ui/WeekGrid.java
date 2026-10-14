package cn.nua.wenguan.schedule.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import cn.nua.wenguan.schedule.AppearanceSettings;
import cn.nua.wenguan.schedule.R;
import cn.nua.wenguan.schedule.data.ScheduleRepository;
import cn.nua.wenguan.schedule.model.CourseSession;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Native clickable blocks preserve accessibility and avoid a nested vertical scroller. */
public final class WeekGrid {
    private static final String[] DAYS = {"一", "二", "三", "四", "五", "六", "日"};
    private static final int[] LIGHT = {0xFF8BCAE8, 0xFFF2B7C7, 0xFF99D6D1, 0xFFC4B4F0, 0xFFEAC28B, 0xFFAABFEF, 0xFFECA891};
    private static final int[] DARK = {0xFF275775, 0xFF704153, 0xFF255D59, 0xFF514270, 0xFF695333, 0xFF394D78, 0xFF724838};
    private WeekGrid() { }
    public static View create(Context c, LocalDate selected, List<CourseSession> sessions) {
        int axis = dp(c, 48), row = dp(c, 82), header = dp(c, 56);
        int available = c.getResources().getDisplayMetrics().widthPixels - dp(c, 40) - axis;
        // Keep seven columns on ordinary phones; allow horizontal scroll on very narrow screens.
        int col = Math.max(1, available / 7);
        int width = axis + col * 7, height = header + row * PeriodGrid.COUNT;
        LocalDate monday = ScheduleRepository.weekMonday(selected);
        LinearLayout body = new LinearLayout(c); body.setOrientation(LinearLayout.VERTICAL);
        TextView tip = text(c, "左右滑动切换周次 · 点击课程看详情", 12, c.getColor(R.color.text_secondary));
        tip.setPadding(0, dp(c, 6), 0, dp(c, 8)); body.addView(tip);
        FrameLayout grid = new FrameLayout(c);
        grid.setBackgroundColor(c.getColor(R.color.surface));
        body.addView(grid, new LinearLayout.LayoutParams(width, height));
        add(grid, text(c, selected.getMonthValue() + "月\n节次", 12, c.getColor(R.color.text_secondary)), 0, 0, axis, header);
        for (int day = 0; day < 7; day++) {
            LocalDate date = monday.plusDays(day);
            TextView heading = text(c, DAYS[day] + "\n" + date.getDayOfMonth(), 13, c.getColor(R.color.text_primary));
            heading.setGravity(Gravity.CENTER);
            if (date.equals(ScheduleRepository.today())) {
                heading.setTextColor(c.getColor(R.color.brand_navy));
                heading.setTypeface(null, Typeface.BOLD);
                heading.setBackgroundColor(c.getColor(R.color.nav_background));
            }
            heading.setContentDescription(date + " 星期" + DAYS[day]);
            add(grid, heading, axis + day * col, 0, col, header);
            if (date.equals(selected)) {
                View mark = new View(c); mark.setBackgroundColor(c.getColor(R.color.brand_navy));
                add(grid, mark, axis + day * col + dp(c, 4), header - dp(c, 3), col - dp(c, 8), dp(c, 2));
            }
        }
        for (int p = 1; p <= PeriodGrid.COUNT; p++) {
            TextView period = text(c, p + "\n" + PeriodGrid.start(p) + "\n" + PeriodGrid.end(p), 11, c.getColor(R.color.text_secondary));
            period.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            period.setPadding(0, dp(c, 6), 0, 0);
            add(grid, period, 0, header + (p - 1) * row, axis, row);
            View line = new View(c); line.setBackgroundColor(c.getColor(R.color.card_border));
            add(grid, line, axis, header + (p - 1) * row, col * 7, dp(c, 1));
        }
        List<CourseSession> unsupported = new ArrayList<>();
        for (int day = 0; day < 7; day++) {
            List<CourseSession> daily = new ArrayList<>();
            for (CourseSession s : sessions) if (s.getDate().equals(monday.plusDays(day))) {
                if (PeriodGrid.span(s) == null) unsupported.add(s); else daily.add(s);
            }
            // One connected collision group shares column width; no course can cover another.
            for (int i = 0; i < daily.size(); ) {
                int groupEnd = i + 1, last = PeriodGrid.span(daily.get(i))[1];
                while (groupEnd < daily.size() && PeriodGrid.span(daily.get(groupEnd))[0] <= last) {
                    last = Math.max(last, PeriodGrid.span(daily.get(groupEnd))[1]); groupEnd++;
                }
                int lanes = groupEnd - i;
                for (int j = i; j < groupEnd; j++) {
                    CourseSession s = daily.get(j); int[] span = PeriodGrid.span(s);
                    TextView block = text(c, s.getCourse() + "\n\n@" + s.getRoom(), 12, c.getColor(R.color.text_primary));
                    block.setGravity(Gravity.TOP | Gravity.START); block.setPadding(dp(c, 4), dp(c, 6), dp(c, 3), dp(c, 4));
                    block.setTypeface(null, Typeface.BOLD);
                    block.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    int blockHeight = (span[1] - span[0] + 1) * row - dp(c, 6);
                    block.setMaxLines(Math.max(1, (int)((blockHeight - dp(c, 10)) / (14 * c.getResources().getDisplayMetrics().scaledDensity))));
                    GradientDrawable background = new GradientDrawable();
                    int index = Math.floorMod(s.getCourse().hashCode(), LIGHT.length);
                    background.setColor((AppearanceSettings.dark(c) ? DARK : LIGHT)[index]);
                    background.setCornerRadius(dp(c, 7));
                    background.setStroke(dp(c, 1), c.getColor(R.color.card_border)); block.setBackground(background);
                    block.setContentDescription(details(s));
                    block.setOnClickListener(v -> CourseDetailsSheet.show(c, s));
                    int laneWidth = col / lanes;
                    add(grid, block, axis + day * col + (j - i) * laneWidth + dp(c, 2), header + (span[0] - 1) * row + dp(c, 3), Math.max(1, laneWidth - dp(c, 4)), blockHeight);
                }
                i = groupEnd;
            }
        }
        if (!unsupported.isEmpty()) {
            TextView note = text(c, "以下课程的自定义时间不对应标准节次，单独列出避免错位：", 12, c.getColor(R.color.text_secondary)); body.addView(note);
            for (CourseSession s : unsupported) { TextView item=text(c, details(s), 14, c.getColor(R.color.text_primary)); item.setOnClickListener(v->CourseDetailsSheet.show(c,s)); body.addView(item); }
        }
        return body;
    }
    private static String details(CourseSession s) {
        return s.getCourse() + "\n" + s.getDate() + "\n" + s.getTimeRange() + " · " + s.getPeriods() + "\n地点：" + s.getRoom()
                + "\n教师：" + s.getTeacher() + "\n" + s.getType() + " · " + ScheduleRepository.statusAt(s, ScheduleRepository.now()).getLabel();
    }
    private static TextView text(Context c, String value, int size, int color) {
        TextView text = new TextView(c); text.setText(value); text.setTextSize(size); text.setTextColor(color); text.setIncludeFontPadding(false); return text;
    }
    private static void add(FrameLayout parent, View view, int x, int y, int width, int height) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height); params.leftMargin = x; params.topMargin = y; parent.addView(view, params);
    }
    private static int dp(Context c, int value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
}
