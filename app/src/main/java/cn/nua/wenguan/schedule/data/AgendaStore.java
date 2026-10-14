package cn.nua.wenguan.schedule.data;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import cn.nua.wenguan.schedule.model.AgendaEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Separate local data: school sync/clear never removes personal agenda items. */
public final class AgendaStore {
    private AgendaStore() { }
    public static List<AgendaEvent> load(Context c) {
        try {
            String raw=c.getSharedPreferences("agenda_events",0).getString("events","[]");
            if(raw.length()>1024*1024) return null;
            JSONArray array=new JSONArray(raw); if(array.length()>2000)return null;
            List<AgendaEvent> result=new ArrayList<>();
            for(int i=0;i<array.length();i++) {
                JSONObject e=array.getJSONObject(i);
                result.add(new AgendaEvent(e.getString("id"),e.getString("title"),e.optString("place"),LocalDate.parse(e.getString("date")),LocalTime.parse(e.getString("start")),LocalTime.parse(e.getString("end")),e.optBoolean("allDay"),e.optBoolean("done"),e.optBoolean("remind")));
            }
            return result;
        } catch(Exception unreadable) { return null; }
    }
    public static boolean put(Context c, AgendaEvent event, boolean delete) {
        List<AgendaEvent> events=load(c); if(events==null)return false;
        events.removeIf(e->e.id.equals(event.id)); if(!delete)events.add(event);
        if(events.size()>2000)return false;
        try {
            JSONArray array=new JSONArray();
            for(AgendaEvent e:events) array.put(new JSONObject().put("id",e.id).put("title",e.title).put("place",e.place).put("date",e.date.toString()).put("start",e.start.toString()).put("end",e.end.toString()).put("allDay",e.allDay).put("done",e.done).put("remind",e.remind));
            return c.getSharedPreferences("agenda_events",0).edit().putString("events",array.toString()).commit();
        } catch(Exception failed) { return false; }
    }
}
