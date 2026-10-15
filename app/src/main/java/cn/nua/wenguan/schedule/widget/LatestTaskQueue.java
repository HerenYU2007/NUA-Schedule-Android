package cn.nua.wenguan.schedule.widget;

import java.util.*;

/** Android-free queue contract: coalesce pending work, never lose a completion callback. */
public final class LatestTaskQueue {
    private final LinkedHashMap<String,Task> pending=new LinkedHashMap<>();
    private boolean running;
    private static final class Task {
        Runnable work;final List<Runnable> finished=new ArrayList<>();
        Task(Runnable work){this.work=work;}
    }
    public synchronized boolean offer(String key,Runnable work,Runnable finished){
        Task task=pending.remove(key);if(task==null)task=new Task(work);else task.work=work;
        pending.put(key,task); // Replacement is the latest event, not the earliest one.
        if(finished!=null)task.finished.add(finished);
        if(running)return false;running=true;return true;
    }
    public synchronized Runnable poll(){
        if(pending.isEmpty()){running=false;return null;}
        Task task=pending.remove(pending.keySet().iterator().next());
        return ()->{try{task.work.run();}finally{for(Runnable done:task.finished)try{done.run();}catch(RuntimeException ignored){}}};
    }
}
