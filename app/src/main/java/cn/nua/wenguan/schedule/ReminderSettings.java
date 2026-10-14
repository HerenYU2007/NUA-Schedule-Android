package cn.nua.wenguan.schedule;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public final class ReminderSettings {
    private ReminderSettings() { }
    public static void show(Activity a) {
        LinearLayout body = new LinearLayout(a); body.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * a.getResources().getDisplayMetrics().density);
        body.setPadding(pad, pad, pad, pad);
        CheckBox enabled = new CheckBox(a); enabled.setText("开启上课提醒"); enabled.setChecked(ReminderScheduler.enabled(a)); body.addView(enabled);
        Spinner choice = new Spinner(a);
        int[] minutes = {5, 10, 15, 20, 30};
        choice.setAdapter(new ArrayAdapter<>(a, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"提前5分钟", "提前10分钟", "提前15分钟", "提前20分钟", "提前30分钟"}));
        for(int n=0;n<minutes.length;n++) if(minutes[n]==ReminderScheduler.minutesBefore(a)) choice.setSelection(n);
        body.addView(choice);
        NotificationManager manager = a.getSystemService(NotificationManager.class);
        TextView hint = new TextView(a);
        hint.setText("\n通知权限：" + (manager != null && manager.areNotificationsEnabled() ? "已允许" : "未允许，开启后需授权")
                + "\n准时提醒：" + (ReminderScheduler.exactAllowed(a) ? "已允许" : "未允许，可点击下方授权")
                + "\n省电策略、强制停止和系统限制仍可能延迟提醒。关闭通知不会删除课程。");
        body.addView(hint);
        new AlertDialog.Builder(a).setTitle("上课提醒").setView(body).setNegativeButton("取消", null)
                .setNeutralButton("提醒权限", (d,w) -> {
                    try {
                        if(Build.VERSION.SDK_INT >= 31 && !ReminderScheduler.exactAllowed(a))
                            a.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + a.getPackageName())));
                        else a.startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, a.getPackageName()));
                    } catch(RuntimeException ignored) { }
                }).setPositiveButton("保存", (d,w) -> {
                    ReminderScheduler.configure(a, enabled.isChecked(), minutes[choice.getSelectedItemPosition()]);
                    if(enabled.isChecked() && Build.VERSION.SDK_INT >= 33
                            && a.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED)
                        a.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 7233);
                }).show();
    }
}
