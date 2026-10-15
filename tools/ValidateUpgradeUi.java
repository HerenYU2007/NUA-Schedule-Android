import cn.nua.wenguan.schedule.ui.*;
import cn.nua.wenguan.schedule.widget.*;
import cn.nua.wenguan.schedule.UpdateCheckPolicy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public final class ValidateUpgradeUi {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void close(float a,float b,String message){check(Math.abs(a-b)<.01f,message);}
    public static void main(String[] arguments)throws Exception{
        for(float width:new float[]{240,320,393,412,600,800,1024,1600})for(int step=-150;step<=150;step++){
            float[] x=WeekPagerPolicy.positions(width,width*step/100f);
            close(x[0]+width,x[1],"left and current edges meet");close(x[1]+width,x[2],"current and right edges meet");
            check(x[1]>=-width&&x[1]<=width,"drag bounded to one page");
            for(int direction=-1;direction<=1;direction++){
                long duration=WeekPagerPolicy.duration(width,x[1],-direction*width);check(duration>=80&&duration<=220,"bounded animation duration");
            }
        }
        for(boolean outside:new boolean[]{false,true})for(int slop=1;slop<40;slop++){
            OutsideTapPolicy p=new OutsideTapPolicy();p.down(10,10,slop,outside);check(p.up(10,10)==outside,"only tap begun outside cancels");check(!p.up(10,10),"UP consumed once");
            p.down(10,10,slop,true);p.move(10,10+slop+1);p.move(10,10);check(!p.up(10,10),"drag returning to origin is not a tap");
            p.down(10,10,slop,true);p.cancel();check(!p.up(10,10),"cancel/multi-touch not a tap");
            p.down(10,10,slop,true);check(!p.up(10+slop+1,10),"UP displacement checked even if MOVE absent");
        }
        check(WidgetHostPolicy.singleLayout(0)&&WidgetHostPolicy.singleLayout(599),"phone/unknown uses one complete root");
        check(!WidgetHostPolicy.singleLayout(600)&&!WidgetHostPolicy.singleLayout(840),"tablet responsive path retained");
        check(Arrays.equals(WidgetHostPolicy.currentSize(240,400,120,240,250,120,false),new int[]{240,240}),"portrait dimensions");
        check(Arrays.equals(WidgetHostPolicy.currentSize(240,400,120,240,250,120,true),new int[]{400,120}),"landscape dimensions");
        for(int a:new int[]{-5,0,100,600})for(int b:new int[]{0,100,800})for(boolean landscape:new boolean[]{false,true}){
            int[] size=WidgetHostPolicy.currentSize(a,b,b,a,250,120,landscape);check(size[0]>0&&size[1]>0,"invalid/inverted host options normalized");
        }
        for(int installed=1;installed<100;installed++)for(int remote=1;remote<100;remote++)check(UpdateCheckPolicy.hasUpdate(installed,remote)==(remote>installed),"older/equal verified releases mean no update");
        for(long version:new long[]{-1,0})try{UpdateCheckPolicy.hasUpdate(32,version);throw new AssertionError("invalid offered code accepted");}catch(IllegalArgumentException expected){checks++;}
        LatestTaskQueue q=new LatestTaskQueue();List<String> work=new ArrayList<>(),done=new ArrayList<>();
        check(q.offer("A",()->work.add("oldA"),()->done.add("oldA")),"first request starts drain");
        check(!q.offer("B",()->work.add("B"),()->done.add("B")),"one active drain");
        check(!q.offer("A",()->work.add("newA"),()->done.add("newA")),"replacement coalesced");
        Runnable task;while((task=q.poll())!=null)task.run();
        check(work.equals(Arrays.asList("B","newA")),"latest occurrence order retained");
        check(done.equals(Arrays.asList("B","oldA","newA")),"all merged receiver completions run once");
        check(q.offer("failure",()->{throw new IllegalStateException();},()->done.add("failure")),"idle queue restarts");
        try{q.poll().run();throw new AssertionError("fixture failure missing");}catch(IllegalStateException expected){check(done.contains("failure"),"completion runs on failure");}
        check(q.poll()==null,"failed task does not poison queue state");
        // Concurrent producers with one consumer: no abandoned completion or restart race.
        LatestTaskQueue concurrent=new LatestTaskQueue();AtomicInteger completions=new AtomicInteger();
        ExecutorService consumer=Executors.newSingleThreadExecutor(),producers=Executors.newFixedThreadPool(4);
        for(int producer=0;producer<4;producer++)producers.submit(()->{
            for(int n=0;n<1000;n++)if(concurrent.offer("key"+(n%8),()->{},completions::incrementAndGet))consumer.execute(()->{Runnable item;while((item=concurrent.poll())!=null)item.run();});
        });
        producers.shutdown();check(producers.awaitTermination(10,TimeUnit.SECONDS),"producers finish");consumer.shutdown();check(consumer.awaitTermination(10,TimeUnit.SECONDS),"consumer finishes");
        check(completions.get()==4000,"every asynchronous receiver completion retained");
        check(concurrent.poll()==null,"drained queue empty");
        System.out.println("Upgrade/UI policies: "+checks+" checks passed. Device rendering, upgrade delivery and FPS NOT exercised.");
    }
}
