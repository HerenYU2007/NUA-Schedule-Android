import cn.nua.wenguan.schedule.ui.WeekSwipePolicy;
import cn.nua.wenguan.schedule.model.AgendaEvent;
import cn.nua.wenguan.schedule.data.SchoolSyncPolicy;
import java.time.LocalDate;
import java.time.LocalTime;
public final class ValidateUiPolicy {
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        require(WeekSwipePolicy.direction(-100,10,48)==1,"left to next week");
        require(WeekSwipePolicy.direction(100,10,48)==-1,"right to previous week");
        require(WeekSwipePolicy.direction(10,100,48)==0,"vertical scroll stays vertical");
        require(WeekSwipePolicy.direction(10,5,48)==0,"tap/jitter does not switch");
        require(WeekSwipePolicy.direction(Float.NaN,0,48)==0,"invalid gesture safe");
        AgendaEvent all=new AgendaEvent("id","test","place",LocalDate.of(2026,10,14),null,null,true,false,true);
        require(all.allDay&&all.start.equals(LocalTime.of(9,0)),"all-day reminder morning");
        require(all.completed(true).done&&!all.done,"completion does not mutate prior event");
        boolean rejected=false;try{new AgendaEvent("id","test","",all.date,LocalTime.of(23,0),LocalTime.of(1,0),false,false,false);}catch(IllegalArgumentException error){rejected=true;}require(rejected,"cross-day time must be explicit/rejected");
        require(SchoolSyncPolicy.portalRetryFor("https://i.nua.edu.cn/?ticket=secret").equals("https://i.nua.edu.cn/"),"portal retry never carries ticket");
        require(SchoolSyncPolicy.portalRetryFor("https://c.nua.edu.cn/cas/login").isEmpty(),"CAS form never selected for retry");
        require(SchoolSyncPolicy.portalRetryFor("https://jwnew.nua.edu.cn/sso.jsp?ticket=secret").isEmpty(),"SSO callback never replayed");
        require(SchoolSyncPolicy.portalRetryFor("https://v.nua.edu.cn/https/0123456789abcdef0123456789abcdef/").endsWith("/"),"same observed gateway route");
        System.out.println("UI/session policies passed: swipe directions and tap/scroll isolation, event validation, query-free same-channel retry.");
    }
}
