package cn.nua.wenguan.schedule;

import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.util.*;
import java.util.concurrent.Callable;

/** Per-check cancellation only; APK download connections are never in this scope. */
public final class UpdateCheckConnections {
    private static final ThreadLocal<UpdateCheckConnections> CURRENT=new ThreadLocal<>();
    private final Set<HttpURLConnection> active=new HashSet<>();
    private volatile boolean cancelled;
    public boolean isCancelled(){return cancelled;}
    public <T> T call(Callable<T> action)throws Exception{
        UpdateCheckConnections previous=CURRENT.get();CURRENT.set(this);
        try{ensureOpen();T value=action.call();ensureOpen();return value;}
        finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
    }
    public static void ensureOpen()throws InterruptedIOException{
        UpdateCheckConnections scope=CURRENT.get();
        if(scope!=null&&(scope.cancelled||Thread.currentThread().isInterrupted()))throw new InterruptedIOException("Update check cancelled");
    }
    public static void register(HttpURLConnection connection)throws InterruptedIOException{
        UpdateCheckConnections scope=CURRENT.get();if(scope==null)return;
        synchronized(scope){if(!scope.cancelled){scope.active.add(connection);return;}}
        connection.disconnect();throw new InterruptedIOException("Update check cancelled");
    }
    public static void disconnect(HttpURLConnection connection){
        UpdateCheckConnections scope=CURRENT.get();if(scope!=null)synchronized(scope){scope.active.remove(connection);}
        connection.disconnect();
    }
    public void cancel(){
        List<HttpURLConnection> closing;
        synchronized(this){cancelled=true;closing=new ArrayList<>(active);active.clear();}
        for(HttpURLConnection connection:closing)try{connection.disconnect();}catch(RuntimeException ignored){/* Continue closing other probes. */}
    }
}
