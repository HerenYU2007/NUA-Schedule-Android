package cn.nua.wenguan.schedule.widget;

import android.content.Context;
import java.util.*;
import java.util.concurrent.*;

/** All bitmap/data/IPC work shares one off-main queue. Latest same-key update wins. */
public final class WidgetWork {
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor(r->new Thread(r,"widget-render"));
    private static final LatestTaskQueue PENDING=new LatestTaskQueue();
    private WidgetWork() { }
    public static boolean sharedRefresh(android.content.Intent event){return android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(event.getAction())||android.content.Intent.ACTION_MY_PACKAGE_REPLACED.equals(event.getAction());}
    public static String eventKey(Class<?> type,android.content.Intent event){
        String action=event.getAction();String key=type.getName()+":"+action+":"+event.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,-1);
        if(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_RESTORED.equals(action)||android.appwidget.AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action))key+=":"+java.util.UUID.randomUUID();
        return key;
    }
    public static void enqueue(Context context,String key,Runnable work,Runnable finished){
        Context app=context.getApplicationContext();
        if(PENDING.offer(key,work,finished))WORK.execute(()->drain(app));
    }
    private static void drain(Context c){
        for(;;){
            Runnable task=PENDING.poll();if(task==null)return;
            try{task.run();}catch(RuntimeException failure){WidgetDiagnostics.failure(c,"worker",-1,failure);}
        }
    }
    public static void refresh(Context context){
        Context c=context.getApplicationContext();enqueue(c,"all-widgets",()->WidgetRecovery.refreshNow(c),null);
    }
}
