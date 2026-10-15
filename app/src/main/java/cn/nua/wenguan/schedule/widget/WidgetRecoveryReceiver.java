package cn.nua.wenguan.schedule.widget;
import android.content.*;
public final class WidgetRecoveryReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent){
        String action=intent.getAction();
        if(!Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)&&!Intent.ACTION_BOOT_COMPLETED.equals(action)&&!Intent.ACTION_USER_UNLOCKED.equals(action))return;
        PendingResult pending=goAsync();
        try{WidgetRecovery.begin(c,pending::finish);}catch(RuntimeException unavailable){pending.finish();}
    }
}
