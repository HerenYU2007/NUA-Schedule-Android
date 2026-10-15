package cn.nua.wenguan.schedule;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.*;
import cn.nua.wenguan.schedule.ui.EditUi;
public final class BackgroundHelp {
    private BackgroundHelp() { }
    public static void show(Activity a){PowerManager power=a.getSystemService(PowerManager.class);boolean ignored=power!=null&&power.isIgnoringBatteryOptimizations(a.getPackageName());LinearLayout body=EditUi.card(a);body.addView(EditUi.text(a,"电池优化："+(ignored?"已放行":"系统默认管理")+"\n通知权限："+(NotificationAccess.allowed(a)?"已允许":"未允许")+"\n准时提醒："+(ReminderScheduler.exactAllowed(a)?"已允许":"未允许")+"\n\n若小组件或提醒不及时，可在系统中允许后台活动、取消对本应用的电池限制；部分手机还需单独允许自启动。菜单因品牌不同。\n不会自动改系统设置，也不保证省电、勿扰、强制停止下始终准时。",15,false));EditUi.row(a,body,"电池优化设置","由你选择是否放行",()->open(a,new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));EditUi.row(a,body,"应用详情","查看后台与自启动设置",()->open(a,new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+a.getPackageName()))));EditUi.row(a,body,"通知设置","查看通知通道",()->NotificationAccess.request(a));new AlertDialog.Builder(a).setTitle("让小组件与提醒更稳定").setView(body).setPositiveButton("完成",(d,w)->ScheduleChanges.refresh(a)).show();}
    private static void open(Activity a,Intent i){try{a.startActivity(i);}catch(RuntimeException missing){Toast.makeText(a,"当前系统没有此入口，请在设置中搜索本应用",Toast.LENGTH_LONG).show();}}
}
