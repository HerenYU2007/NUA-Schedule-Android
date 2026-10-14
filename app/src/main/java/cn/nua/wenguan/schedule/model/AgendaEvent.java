package cn.nua.wenguan.schedule.model;
import java.time.LocalDate;
import java.time.LocalTime;
public final class AgendaEvent {
    public final String id, title, place;
    public final LocalDate date;
    public final LocalTime start, end;
    public final boolean allDay, done, remind;
    public AgendaEvent(String id,String title,String place,LocalDate date,LocalTime start,LocalTime end,boolean allDay,boolean done,boolean remind) {
        if(id==null||id.isEmpty()||title==null||title.trim().isEmpty()||title.length()>80||place==null||place.length()>200||date==null) throw new IllegalArgumentException("请填写标题（最多80字）并检查地点和日期");
        if(!allDay&&(start==null||end==null||!end.isAfter(start))) throw new IllegalArgumentException("结束时间须晚于开始时间；暂不支持跨日时间段");
        this.id=id;this.title=title.trim();this.place=place;this.date=date;this.start=allDay?LocalTime.of(9,0):start;this.end=allDay?LocalTime.of(23,59):end;this.allDay=allDay;this.done=done;this.remind=remind;
    }
    public AgendaEvent completed(boolean value) { return new AgendaEvent(id,title,place,date,start,end,allDay,value,remind); }
    public String timeText() { return allDay?"全天":start+"–"+end; }
}
