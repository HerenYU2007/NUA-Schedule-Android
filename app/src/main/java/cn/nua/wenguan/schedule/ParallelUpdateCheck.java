package cn.nua.wenguan.schedule;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.ToLongFunction;

/** Bounded completion-order race. A cached old manifest must not hide a newer one. */
public final class ParallelUpdateCheck {
    public static final int PARALLELISM=6;
    public static final long TIMEOUT_MS=30000;
    private ParallelUpdateCheck() { }
    public interface Probe<T> { T fetch(String source) throws Exception; }
    private static final class Result<T> {
        final T manifest;final List<String> extra;final boolean discovery;
        Result(T manifest,List<String> extra,boolean discovery){this.manifest=manifest;this.extra=extra;this.discovery=discovery;}
    }
    /** Mix reliable mirrors and all official paths in the first batch. */
    public static List<String> prioritize(List<String> ranked,List<String> official){
        LinkedHashSet<String> ordered=new LinkedHashSet<>();
        for(String route:ranked)if(!official.contains(route)){ordered.add(route);if(ordered.size()==2)break;}
        ordered.addAll(official);ordered.addAll(ranked);return new ArrayList<>(ordered);
    }
    public static <T> T fetch(List<String> routes,long installed,Probe<T> probe,ToLongFunction<T> version,
            Callable<List<String>> discover,Runnable cancelRequests,long timeoutMs)throws Exception{
        if(installed<=0||timeoutMs<=0)throw new IllegalArgumentException("Invalid check bounds");
        long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        ExecutorService pool=Executors.newFixedThreadPool(PARALLELISM+(discover==null?0:1),task->{Thread thread=new Thread(task,"update-probe");thread.setDaemon(true);return thread;});
        CompletionService<Result<T>> complete=new ExecutorCompletionService<>(pool);
        List<Future<Result<T>>> tasks=new ArrayList<>();
        Set<String> seen=new HashSet<>();ArrayDeque<String> pending=new ArrayDeque<>();
        for(String route:routes)if(route!=null&&seen.add(route))pending.add(route);
        int running=0;boolean discovering=discover!=null;T best=null;long bestVersion=0;
        try{
            if(discover!=null)tasks.add(complete.submit(()->{try{return new Result<>(null,discover.call(),true);}catch(Exception unavailable){return new Result<>(null,Collections.emptyList(),true);}}));
            while(true){
                while(running<PARALLELISM&&!pending.isEmpty()&&System.nanoTime()<deadline){
                    String route=pending.remove();
                    tasks.add(complete.submit(()->{try{return new Result<>(probe.fetch(route),null,false);}catch(Exception unavailable){return new Result<>(null,null,false);}}));running++;
                }
                if(running==0&&!discovering)break;
                long remaining=deadline-System.nanoTime();if(remaining<=0)break;
                Future<Result<T>> finished=complete.poll(remaining,TimeUnit.NANOSECONDS);if(finished==null)break;
                Result<T> result=finished.get();
                if(result.discovery){discovering=false;if(result.extra!=null)for(String route:result.extra)if(route!=null&&seen.add(route))pending.add(route);}
                else{
                    running--;
                    if(result.manifest!=null){long offered=version.applyAsLong(result.manifest);
                        if(offered<=0)continue;
                        if(offered>installed)return result.manifest;
                        if(best==null||offered>bestVersion){best=result.manifest;bestVersion=offered;}
                    }
                }
            }
            if(best!=null)return best;
            throw new IOException("No verified update manifest");
        }finally{
            for(Future<?> task:tasks)task.cancel(true);
            pool.shutdownNow();cancelRequests.run();
        }
    }
}
