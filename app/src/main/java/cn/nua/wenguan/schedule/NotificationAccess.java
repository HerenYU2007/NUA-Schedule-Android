package cn.nua.wenguan.schedule;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
public final class NotificationAccess {
    private NotificationAccess() { }
    public static final String CLASSES="class_reminders",AGENDA="agenda_reminders",ALARM="class_alarm_v1";
    public static boolean allowed(Context c){NotificationManager m=c.getSystemService(NotificationManager.class);return m!=null&&m.areNotificationsEnabled()&&(Build.VERSION.SDK_INT<33||c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")==android.content.pm.PackageManager.PERMISSION_GRANTED);}
    public static void channels(Context c){NotificationManager m=c.getSystemService(NotificationManager.class);if(m==null)return;m.createNotificationChannel(new NotificationChannel(CLASSES,"上课通知提醒",NotificationManager.IMPORTANCE_HIGH));m.createNotificationChannel(new NotificationChannel(AGENDA,"日程提醒",NotificationManager.IMPORTANCE_HIGH));NotificationChannel alarm=new NotificationChannel(ALARM,"课程闹钟",NotificationManager.IMPORTANCE_HIGH);alarm.setSound(null,null);m.createNotificationChannel(alarm);}
    public static boolean channelAllowed(Context c,String id){if(!allowed(c))return false;NotificationChannel channel=c.getSystemService(NotificationManager.class).getNotificationChannel(id);return channel==null||channel.getImportance()!=NotificationManager.IMPORTANCE_NONE;}
    public static boolean post(Context c,String channel,int id,String title,String body,boolean agenda){try{channels(c);if(!channelAllowed(c,channel))return false;PendingIntent open=PendingIntent.getActivity(c,agenda?8429:7231,new Intent(c,MainActivity.class).putExtra("open_agenda",agenda),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification n=new Notification.Builder(c,channel).setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setCategory(Notification.CATEGORY_REMINDER).setVisibility(Notification.VISIBILITY_PRIVATE).setAutoCancel(true).setContentIntent(open).build();c.getSystemService(NotificationManager.class).notify(id&0x7fffffff,n);return true;}catch(RuntimeException denied){return false;}}
    public static void request(Activity a){if(Build.VERSION.SDK_INT>=33&&a.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)a.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},7233);else openSettings(a);}
    public static void openSettings(Activity a){try{a.startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,a.getPackageName()));}catch(RuntimeException unsupported){try{a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+a.getPackageName())));}catch(RuntimeException ignored){}}}
}
