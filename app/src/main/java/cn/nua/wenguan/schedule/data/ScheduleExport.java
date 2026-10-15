package cn.nua.wenguan.schedule.data;
import cn.nua.wenguan.schedule.model.ScheduleRule;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableWorkbook;
import jxl.write.WritableSheet;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;

/** Offline exports. JSON is an application backup, not an undocumented school/wakeup format. */
public final class ScheduleExport {
    private ScheduleExport() { }
    private static int number(JSONObject value,String key) throws JSONException {Object raw=value.get(key);if(!(raw instanceof Number)||!Double.isFinite(((Number)raw).doubleValue())||((Number)raw).doubleValue()!=((Number)raw).intValue())throw new JSONException("备份数字字段格式错误："+key);return ((Number)raw).intValue();}
    private static String text(JSONObject value,String key) throws JSONException {Object raw=value.get(key);if(!(raw instanceof String))throw new JSONException("备份文本字段格式错误："+key);return (String)raw;}
    public static JSONObject json(List<ScheduleRule> rules,LocalDate start) throws JSONException {
        JSONArray items=new JSONArray();for(ScheduleRule r:rules)items.put(new JSONObject().put("id",r.getId()).put("from",r.getFromWeek()).put("to",r.getToWeek()).put("day",r.getDayOfWeek().getValue()).put("start",r.getStartTime().toString()).put("end",r.getEndTime().toString()).put("periods",r.getPeriods()).put("course",r.getCourse()).put("room",r.getRoom()).put("teacher",r.getTeacher()).put("type",r.getType()).put("color",r.getColor()).put("credits",r.getCredits()).put("note",r.getNote()));
        return new JSONObject().put("format","nua-schedule-backup").put("schema",1).put("startDate",start.toString()).put("rules",items);
    }
    public static List<ScheduleRule> parse(JSONObject value) throws JSONException {
        if(!"nua-schedule-backup".equals(text(value,"format"))||number(value,"schema")!=1)throw new JSONException("不是本应用支持的课表备份");LocalDate start=LocalDate.parse(text(value,"startDate"));if(start.getYear()<1900||start.getYear()>2200)throw new JSONException("开学日期不受支持");JSONArray a=value.getJSONArray("rules");if(a.length()==0||a.length()>5000)throw new JSONException("备份为空或过大");List<ScheduleRule> result=new ArrayList<>();Set<String> ids=new HashSet<>();
        for(int n=0;n<a.length();n++){JSONObject e=a.getJSONObject(n);ScheduleRule r=new ScheduleRule(number(e,"from"),number(e,"to"),DayOfWeek.of(number(e,"day")),text(e,"start"),text(e,"end"),text(e,"periods"),text(e,"course"),text(e,"room"),text(e,"type"),text(e,"teacher"),text(e,"id"),e.has("color")?number(e,"color"):0,e.has("credits")?text(e,"credits"):"",e.has("note")?text(e,"note"):"");if(!ids.add(r.getId()))throw new JSONException("备份存在重复时段");result.add(r);}return result;
    }
    public static void xls(OutputStream stream,List<ScheduleRule> rules,LocalDate start)throws Exception{
        WritableWorkbook book=Workbook.createWorkbook(stream);try{WritableSheet sheet=book.createSheet("课程安排",0);WritableCellFormat header=new WritableCellFormat(new WritableFont(WritableFont.ARIAL,11,WritableFont.BOLD));header.setBackground(jxl.format.Colour.PALE_BLUE);header.setWrap(true);WritableCellFormat body=new WritableCellFormat();body.setWrap(true);String[] columns={"课程名称","开始周","结束周","星期","节次","开始时间","结束时间","地点","教师","学分","备注"};for(int n=0;n<columns.length;n++){sheet.addCell(new Label(n,0,columns[n],header));sheet.setColumnView(n,n==0||n==10?28:n==7?18:12);}sheet.getSettings().setVerticalFreeze(1);int row=1;for(ScheduleRule r:rules){String[] cells={r.getCourse(),String.valueOf(r.getFromWeek()),String.valueOf(r.getToWeek()),"周"+"一二三四五六日".charAt(r.getDayOfWeek().getValue()-1),r.getPeriods(),r.getStartTime().toString(),r.getEndTime().toString(),r.getRoom(),r.getTeacher(),r.getCredits(),r.getNote()};for(int n=0;n<cells.length;n++)sheet.addCell(new Label(n,row,cells[n],body));row++;}WritableSheet info=book.createSheet("使用说明",1);info.setColumnView(0,90);info.addCell(new Label(0,0,"开学日期："+start+"；该日期所在周为第1周。"));info.addCell(new Label(0,1,"这是一行一个时段的可读表格；回导本应用请使用另行导出的JSON备份。"));info.addCell(new Label(0,2,"仅导出课程，不包含日程、提醒设置、学校账号、密码或Cookie。"));book.write();}finally{book.close();}
    }
    public static String calendar(List<ScheduleRule> rules,LocalDate start){StringBuilder out=new StringBuilder();line(out,"BEGIN:VCALENDAR");line(out,"VERSION:2.0");line(out,"PRODID:-//NUA Schedule//Personal Courses//ZH-CN");line(out,"CALSCALE:GREGORIAN");int count=0;DateTimeFormatter utc=DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);String stamp=utc.format(Instant.now());LocalDate monday=ScheduleRepository.weekMonday(start);
        long total=0;for(ScheduleRule r:rules)total+=r.getToWeek()-r.getFromWeek()+1L;if(total>10000)throw new IllegalArgumentException("课程次数过多，请分学期导出");
        for(ScheduleRule r:rules)for(int week=r.getFromWeek();week<=r.getToWeek();week++){LocalDate date=monday.plusWeeks(week-1L).plusDays(r.getDayOfWeek().getValue()-1L);line(out,"BEGIN:VEVENT");line(out,"UID:"+r.getId()+"-"+date+"@nua-schedule.local");line(out,"DTSTAMP:"+stamp);line(out,"DTSTART:"+utc.format(date.atTime(r.getStartTime()).atZone(ScheduleRepository.SCHOOL_ZONE)));line(out,"DTEND:"+utc.format(date.atTime(r.getEndTime()).atZone(ScheduleRepository.SCHOOL_ZONE)));line(out,"SUMMARY:"+escape(r.getCourse()));line(out,"LOCATION:"+escape(r.getRoom()));line(out,"DESCRIPTION:"+escape(r.getPeriods()+" · 教师："+r.getTeacher()+"\n"+r.getNote()));line(out,"END:VEVENT");}line(out,"END:VCALENDAR");return out.toString();
    }
    private static String escape(String value){return value.replace("\\","\\\\").replace("\r\n","\n").replace("\r","\n").replace("\n","\\n").replace(";","\\;").replace(",","\\,");}
    private static void line(StringBuilder out,String value){int bytes=0;for(int index=0;index<value.length();){int cp=value.codePointAt(index);String part=new String(Character.toChars(cp));int size=part.getBytes(StandardCharsets.UTF_8).length;if(bytes+size>75){out.append("\r\n ");bytes=1;}out.append(part);bytes+=size;index+=Character.charCount(cp);}out.append("\r\n");}
}
