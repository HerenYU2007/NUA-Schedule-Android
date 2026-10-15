package cn.nua.wenguan.schedule.widget;
import android.app.job.*;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Future;
/** Two bounded post-upgrade redraws, not background academic synchronization. */
public final class WidgetRecoveryJobService extends JobService {
    private Future<?> work;private JobParameters active;
    private final Handler main=new Handler(Looper.getMainLooper());
    @Override public boolean onStartJob(JobParameters params){
        active=params;long generation=params.getExtras().getLong("generation");int attempt=params.getExtras().getInt("attempt");
        work=WidgetRecovery.redraw(this,generation,attempt,()->main.post(()->{
            if(active!=params)return;
            active=null;jobFinished(params,false);WidgetRecovery.next(this,generation,attempt);
        }));
        return true;
    }
    @Override public boolean onStopJob(JobParameters params){active=null;if(work!=null)work.cancel(true);return false;}
    @Override public void onDestroy(){active=null;if(work!=null)work.cancel(true);main.removeCallbacksAndMessages(null);super.onDestroy();}
}
