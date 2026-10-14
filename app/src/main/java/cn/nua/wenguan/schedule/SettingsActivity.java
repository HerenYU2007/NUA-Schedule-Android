package cn.nua.wenguan.schedule;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.time.LocalDate;
import java.util.Date;

import cn.nua.wenguan.schedule.data.ScheduleRepository;
import cn.nua.wenguan.schedule.data.ScheduleRuleStore;
import cn.nua.wenguan.schedule.widget.TodayScheduleWidgetProvider;

public class SettingsActivity extends AppearanceActivity {
    private TextView status;
    private View clearSchedule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        status = findViewById(R.id.settings_status);
        clearSchedule = findViewById(R.id.clear_schedule);

        findViewById(R.id.set_school_start).setOnClickListener(v -> showSchoolStartPicker());
        findViewById(R.id.add_widget).setOnClickListener(v -> addDesktopWidget());
        findViewById(R.id.reminder_button).setOnClickListener(v -> ReminderSettings.show(this));
        findViewById(R.id.manual_import).setOnClickListener(v -> startActivity(new Intent(this, ScheduleSyncActivity.class).putExtra("manual_import", true)));
        findViewById(R.id.theme_settings).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("界面主题").setSingleChoiceItems(new String[]{"跟随系统", "浅色", "深色"}, AppearanceSettings.theme(this), (dialog, which) -> {
                    if (AppearanceSettings.saveTheme(this, which)) { dialog.dismiss(); recreate(); }
                    else Toast.makeText(this, "主题保存失败", Toast.LENGTH_SHORT).show();
                }).setNegativeButton("取消", null).show());
        findViewById(R.id.mode_settings).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("课表展示模式").setSingleChoiceItems(new String[]{"列表课表", "竖条周课表"}, AppearanceSettings.grid(this) ? 1 : 0, (dialog, which) -> {
                    if (AppearanceSettings.saveGrid(this, which == 1)) { dialog.dismiss(); updateUi(); }
                    else Toast.makeText(this, "显示模式保存失败", Toast.LENGTH_SHORT).show();
                }).setNegativeButton("取消", null).show());
        clearSchedule.setOnClickListener(v -> confirmClearSchedule());
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        updateUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();
    }

    private void updateUi() {
        ((TextView) findViewById(R.id.theme_settings)).setText("界面主题：" + AppearanceSettings.themeLabel(this));
        ((TextView) findViewById(R.id.mode_settings)).setText("课表展示：" + (AppearanceSettings.grid(this) ? "竖条周课表" : "列表课表"));
        boolean imported = ScheduleRepository.hasImportedRules();
        LocalDate start = ScheduleRepository.schoolStartDate();
        ((TextView) findViewById(R.id.set_school_start)).setText("开学时间：" + start + " · 修改");
        clearSchedule.setVisibility(imported ? View.VISIBLE : View.GONE);
        long lastSync = ScheduleRuleStore.lastSyncTime(this);
        if (!imported || lastSync <= 0L) {
            status.setText("当前课表为空 · 可在首页登录教务系统获取或手动导入 XLS");
        } else {
            String time = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(new Date(lastSync));
            status.setText("课表已保存 · 最近同步 " + time);
        }
    }

    private void confirmClearSchedule() {
        new AlertDialog.Builder(this)
                .setTitle("清空课表？")
                .setMessage("将删除本应用已导入的课程并恢复为空课表，可重新登录教务系统导入。")
                .setNegativeButton("取消", null)
                .setPositiveButton("清空", (dialog, which) -> {
                    if (!ScheduleRuleStore.clearSchedule(this)) {
                        Toast.makeText(this, "清空失败，原课表保留", Toast.LENGTH_LONG).show();
                        return;
                    }
                    updateUi();
                    requestWidgetRefresh();
                    Toast.makeText(this, "课表已清空", Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void showSchoolStartPicker() {
        LocalDate startDate = ScheduleRepository.schoolStartDate();
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
            LocalDate chosen = LocalDate.of(year, month + 1, day);
            new AlertDialog.Builder(this).setTitle("确认开学时间")
                    .setMessage("开学时间：" + chosen + "\n" + ScheduleRepository.firstWeekText(chosen)
                            + "\n选中的日期所在周将作为第1周。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("保存", (dialog, which) -> {
                        if (!ScheduleRuleStore.setStartDate(this, chosen)) {
                            Toast.makeText(this, "保存失败，原设置保留", Toast.LENGTH_LONG).show();
                            return;
                        }
                        updateUi();
                        requestWidgetRefresh();
                        Toast.makeText(this, "开学时间已更新", Toast.LENGTH_SHORT).show();
                    }).show();
        }, startDate.getYear(), startDate.getMonthValue() - 1, startDate.getDayOfMonth());
        picker.setTitle("选择开学时间 · 所在周为第1周");
        picker.show();
    }

    private void addDesktopWidget() {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        ComponentName component = new ComponentName(this, TodayScheduleWidgetProvider.class);
        try {
            if (manager.isRequestPinAppWidgetSupported()
                    && manager.requestPinAppWidget(component, null, null)) {
                Toast.makeText(this, "请在桌面弹窗中确认添加", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (RuntimeException ignored) { }
        new AlertDialog.Builder(this).setTitle("添加桌面小组件")
                .setMessage("当前桌面不支持 App 发起的添加请求。请在桌面空白处长按，进入“小组件 / Android 小组件”，找到“"
                        + getString(R.string.app_name) + "”并拖到桌面。")
                .setPositiveButton("知道了", null).show();
    }

    private void requestWidgetRefresh() {
        ReminderScheduler.scheduleNext(this);
        Intent intent = new Intent(this, TodayScheduleWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        ComponentName component = new ComponentName(this, TodayScheduleWidgetProvider.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, manager.getAppWidgetIds(component));
        sendBroadcast(intent);
    }
}
