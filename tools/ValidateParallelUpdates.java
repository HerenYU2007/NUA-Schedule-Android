import cn.nua.wenguan.schedule.ParallelUpdateCheck;
import cn.nua.wenguan.schedule.UpdateCheckConnections;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public final class ValidateParallelUpdates {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static void await(CountDownLatch latch)throws Exception{if(!latch.await(2,TimeUnit.SECONDS))throw new AssertionError("Worker did not start concurrently");}
    static Long race(List<String> routes,ParallelUpdateCheck.Probe<Long> probe,Callable<List<String>> discovery,long timeout)throws Exception{
        return ParallelUpdateCheck.fetch(routes,33,probe,Long::longValue,discovery,()->{},timeout);
    }
    public static void main(String[] args)throws Exception{
        List<String> official=List.of("direct","api","raw","github-raw");
        List<String> order=ParallelUpdateCheck.prioritize(List.of("mirror-1","mirror-2","mirror-3","raw","api"),official);
        check(order.subList(0,6).containsAll(official),"Official routes in first six");
        check(new HashSet<>(order).size()==order.size(),"Route deduplication");
        CountDownLatch slowStarted=new CountDownLatch(1),slowCancelled=new CountDownLatch(1);
        Long answer=race(List.of("slow","fast"),route->{if(route.equals("slow")){slowStarted.countDown();try{Thread.sleep(5000);}catch(InterruptedException cancelled){slowCancelled.countDown();throw cancelled;}return 34L;}await(slowStarted);return 35L;},null,2000);
        check(answer==35L,"Completion order, not input order");await(slowCancelled);check(true,"Unused worker cancelled");
        AtomicBoolean newerVisited=new AtomicBoolean();
        answer=race(List.of("old","new"),route->{if(route.equals("old"))return 32L;Thread.sleep(40);newerVisited.set(true);return 34L;},null,1000);
        check(answer==34L&&newerVisited.get(),"Old cache cannot end the race");
        AtomicInteger probes=new AtomicInteger();
        answer=race(List.of("old","old"),route->{probes.incrementAndGet();return route.equals("dynamic")?36L:33L;},()->{Thread.sleep(30);return List.of("old","dynamic","dynamic");},1000);
        check(answer==36L&&probes.get()==2,"Discovery does not block initial checks; duplicate routes ignored");
        answer=race(List.of("equal","old","invalid"),route->{if(route.equals("invalid"))throw new IOException("Unsigned response");return route.equals("equal")?33L:31L;},null,1000);
        check(answer==33L,"No update only after checking other responses");
        AtomicBoolean cancelled=new AtomicBoolean();long started=System.nanoTime();
        try{ParallelUpdateCheck.fetch(List.of("blocked"),33,route->{Thread.sleep(5000);return 34L;},Long::longValue,null,()->cancelled.set(true),80);throw new AssertionError("Expected timeout");}catch(IOException expected){check(cancelled.get(),"Deadline cleanup");}
        check(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started)<1500,"Global deadline bounded");
        try{race(List.of("bad"),route->{throw new IOException("Invalid signature");},null,1000);throw new AssertionError("Untrusted content accepted");}catch(IOException expected){check(true,"All invalid means failure, not no-update");}
        answer=race(List.of("valid"),route->34L,()->{throw new IOException("Directory unavailable");},1000);
        check(answer==34L,"Directory failure cannot stop update");
        AtomicInteger active=new AtomicInteger(),peak=new AtomicInteger();CountDownLatch six=new CountDownLatch(6);
        answer=race(List.of("a","b","c","d","e","f","g","h"),route->{int current=active.incrementAndGet();peak.accumulateAndGet(current,Math::max);six.countDown();try{await(six);return 34L;}finally{active.decrementAndGet();}},null,1500);
        check(answer==34L&&peak.get()==6,"Exactly six manifest probes at most");
        UpdateCheckConnections scope=new UpdateCheckConnections();MockConnection probe=new MockConnection(),apk=new MockConnection();
        UpdateCheckConnections.register(apk);
        scope.call(()->{UpdateCheckConnections.register(probe);return null;});scope.cancel();
        check(probe.disconnected&&!apk.disconnected,"Cancellation never disconnects unrelated APK download");
        MockConnection late=new MockConnection();try{scope.call(()->{UpdateCheckConnections.register(late);return null;});throw new AssertionError("Cancelled scope reused");}catch(InterruptedIOException expected){check(true,"Cancelled scope rejects late work");}
        UpdateCheckConnections normal=new UpdateCheckConnections();MockConnection closed=new MockConnection();normal.call(()->{UpdateCheckConnections.register(closed);UpdateCheckConnections.disconnect(closed);return null;});normal.cancel();check(closed.disconnected,"Normal stream close releases connection");
        Thread.sleep(50);long live=Thread.getAllStackTraces().keySet().stream().filter(thread->thread.isAlive()&&thread.getName().equals("update-probe")).count();
        check(live==0,"No hanging worker in simulated cancellation tests");
        System.out.println("Parallel update race: "+checks+" checks passed (completion order, old cache, six probes, discovery, deadline, cleanup, invalid responses). Simulated transport, not device networking.");
    }
    static final class MockConnection extends HttpURLConnection {
        volatile boolean disconnected;
        MockConnection()throws Exception{super(new URL("https://example.com/"));}
        public void disconnect(){disconnected=true;}
        public boolean usingProxy(){return false;}
        public void connect(){}
    }
}
