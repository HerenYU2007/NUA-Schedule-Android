package cn.nua.wenguan.schedule;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.*;
import android.net.Uri;
import android.os.*;
/** User-enabled bounded alarm audio; no full-screen launch or permanent service. */
public final class CourseAlarmService extends Service {
    private MediaPlayer player;private AudioManager audio;private AudioFocusRequest focus;
    private final Handler handler=new Handler(Looper.getMainLooper());
    @Override public IBinder onBind(Intent i){return null;}
    @Override public int onStartCommand(Intent i,int flags,int startId){
        if(i==null||"STOP".equals(i.getAction())){stopSelf();return START_NOT_STICKY;}
        try{
            NotificationAccess.channels(this);if(!NotificationAccess.channelAllowed(this,NotificationAccess.ALARM)){stopSelf();return START_NOT_STICKY;}
            PendingIntent stop=PendingIntent.getService(this,7239,new Intent(this,CourseAlarmService.class).setAction("STOP"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            PendingIntent open=PendingIntent.getActivity(this,7239,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification n=new Notification.Builder(this,NotificationAccess.ALARM).setSmallIcon(R.drawable.ic_notification).setContentTitle("上课闹钟 · "+i.getStringExtra("title")).setContentText(i.getStringExtra("body")).setCategory(Notification.CATEGORY_ALARM).setVisibility(Notification.VISIBILITY_PRIVATE).setOngoing(true).setContentIntent(open).addAction(new Notification.Action.Builder(null,"停止响铃",stop).build()).build();
            if(Build.VERSION.SDK_INT>=29)startForeground(7239,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);else startForeground(7239,n);
            releaseAudio();AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            audio=getSystemService(AudioManager.class);focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attributes).setOnAudioFocusChangeListener(value->{if(value==AudioManager.AUDIOFOCUS_LOSS)stopSelf();}).build();
            if(audio==null||audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){stopSelf();return START_NOT_STICKY;}
            Uri tone=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);if(tone==null)tone=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            player=new MediaPlayer();player.setAudioAttributes(attributes);player.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);player.setDataSource(this,tone);player.setLooping(true);player.setOnErrorListener((p,what,extra)->{stopSelf();return true;});player.prepare();player.start();handler.removeCallbacksAndMessages(null);handler.postDelayed(this::stopSelf,Math.max(1000,Math.min(60000,i.getIntExtra("duration",60000))));
        }catch(Exception unavailable){stopSelf();}
        return START_NOT_STICKY;
    }
    private void releaseAudio(){if(player!=null){try{player.stop();}catch(RuntimeException ignored){}player.release();player=null;}if(audio!=null&&focus!=null)audio.abandonAudioFocusRequest(focus);focus=null;}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);releaseAudio();stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}
