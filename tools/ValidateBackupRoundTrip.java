import cn.nua.wenguan.schedule.data.ScheduleExport;
import cn.nua.wenguan.schedule.model.ScheduleRule;
import org.json.*;
import java.time.*;
import java.util.*;
public final class ValidateBackupRoundTrip {
    private static int count;
    private static void check(boolean value,String message){count++;if(!value)throw new AssertionError(message);}
    private static JSONObject fixture()throws Exception{return ScheduleExport.json(Arrays.asList(new ScheduleRule(2,15,DayOfWeek.FRIDAY,"08:50","11:40","第2—4节","管理学原理","文313","手动编辑","老师","uuid-test-id",0xffaa3377,"2.5","备注\n😀")),LocalDate.of(2026,9,9));}
    private static void reject(JSONObject json)throws Exception{try{ScheduleExport.parse(json);throw new AssertionError("accepted invalid backup");}catch(JSONException|IllegalArgumentException expected){count++;}}
    public static void main(String[] args)throws Exception{
        JSONObject json=fixture();ScheduleRule rule=ScheduleExport.parse(new JSONObject(json.toString(2))).get(0);check(rule.getColor()==0xffaa3377&&rule.getCredits().equals("2.5")&&rule.getNote().equals("备注\n😀"),"metadata roundtrip");check(rule.getId().equals("uuid-test-id")&&rule.getToWeek()==15&&rule.getDayOfWeek()==DayOfWeek.FRIDAY,"identity/time roundtrip");check(!json.toString().contains("Cookie")&&!json.toString().contains("password"),"no session information");
        for(String field:new String[]{"id","from","course","start","teacher"}){JSONObject invalid=fixture();invalid.getJSONArray("rules").getJSONObject(0).remove(field);reject(invalid);}
        for(Object value:new Object[]{0,54,1.5,"2",JSONObject.NULL}){JSONObject invalid=fixture();invalid.getJSONArray("rules").getJSONObject(0).put("from",value);reject(invalid);}
        JSONObject duplicate=fixture();duplicate.getJSONArray("rules").put(duplicate.getJSONArray("rules").getJSONObject(0));reject(duplicate);
        JSONObject newline=fixture();newline.getJSONArray("rules").getJSONObject(0).put("id","evil\r\nLOCATION:spoof");reject(newline);
        JSONObject beyond=fixture();beyond.put("startDate","+999999999-01-01");reject(beyond);
        JSONObject future=fixture();future.put("schema",2);reject(future);JSONObject fractional=fixture();fractional.put("schema",1.1);reject(fractional);
        JSONObject optional=fixture();JSONObject e=optional.getJSONArray("rules").getJSONObject(0);e.remove("color");e.remove("credits");e.remove("note");check(ScheduleExport.parse(optional).get(0).getNote().isEmpty(),"optional backwards-compatible fields");
        System.out.println("JSON backup: "+count+" JVM checks passed (roundtrip, Unicode, missing/typed/duplicate/malicious ID and out-of-range data). Android storage/lifecycle still needs device tests.");
    }
}
