package cn.nua.wenguan.schedule;
import android.app.*;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import cn.nua.wenguan.schedule.data.*;
import cn.nua.wenguan.schedule.model.*;
import cn.nua.wenguan.schedule.ui.*;
import java.time.*;
import java.util.*;

/** Local edits are explicit; fetching again previews replacement instead of editing the school. */
public final class CourseEditorActivity extends AppearanceActivity {
    private final List<Slot> slots=new ArrayList<>();
    private final Set<String> originalIds=new HashSet<>();
    private LinearLayout body,slotContainer;
    private EditText name,credits;
    private int color;
    private String originalName="";
    private TextView colorRow;
    private static final int[] COLORS={0,0xFF8BCAE8,0xFFF2B7C7,0xFF99D6D1,0xFFC4B4F0,0xFFEAC28B,0xFFAABFEF,0xFFECA891,0xFF4CA99B,0xFFDE708E};
    private static final class Slot {
        String id=UUID.randomUUID().toString(),room="",teacher="",note="",type="手动编辑";
        int from=1,to=16,day=1,first=1,last=2;
        boolean custom; LocalTime start=LocalTime.of(8,0),end=LocalTime.of(9,40);
        Slot copy(){Slot s=new Slot();s.room=room;s.teacher=teacher;s.note=note;s.type=type;s.from=from;s.to=to;s.day=day;s.first=first;s.last=last;s.custom=custom;s.start=start;s.end=end;return s;}
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        List<ScheduleRule> all=ScheduleRuleStore.readRules(this);if(all==null){Toast.makeText(this,"原课表读取失败，已停止编辑",Toast.LENGTH_LONG).show();finish();return;}
        String id=getIntent().getStringExtra("rule_id");ScheduleRule selected=null;
        for(ScheduleRule r:all)if(r.getId().equals(id))selected=r;
        if(id!=null&&selected==null){Toast.makeText(this,"这个上课时段已不存在，请返回重新选择",Toast.LENGTH_LONG).show();finish();return;}
        if(selected!=null){originalName=selected.getCourse();color=selected.getColor();}
        LinearLayout root=EditUi.column(this);root.setBackgroundColor(getColor(R.color.page_background));root.setPadding(dp(18),dp(12),dp(18),0);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        Button back=new Button(this);back.setText("取消");back.setOnClickListener(v->confirmBack());header.addView(back);
        TextView title=EditUi.text(this,selected==null?"添加课程":"修改课程",21,true);title.setGravity(Gravity.CENTER);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button save=new Button(this);save.setText("保存");save.setOnClickListener(v->save());header.addView(save);root.addView(header);
        ScrollView scroll=new ResponsiveScrollView(this);body=EditUi.column(this);body.setPadding(0,dp(12),0,dp(32));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        LinearLayout basic=EditUi.card(this);body.addView(basic);name=EditUi.field(this,basic,"课程名称",selected==null?"":selected.getCourse(),100);
        colorRow=EditUi.row(this,basic,"颜色",color==0?"自动配色":"自选配色",()->pickColor());
        credits=EditUi.field(this,basic,"学分（选填）",selected==null?"":selected.getCredits(),20);credits.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        slotContainer=EditUi.column(this);body.addView(slotContainer);
        if(selected!=null){originalIds.add(selected.getId());slots.add(slot(selected));}
        if(slots.isEmpty()){
            Slot initial=new Slot();
            if(getIntent().hasExtra("draft_date"))try{
                LocalDate date=LocalDate.ofEpochDay(getIntent().getLongExtra("draft_date",Long.MIN_VALUE));
                int week=GridPlacementPolicy.week(date,ScheduleRepository.schoolStartDate());
                int first=getIntent().getIntExtra("draft_first",0),last=getIntent().getIntExtra("draft_last",0);
                if(date.getYear()<1900||date.getYear()>2200||week<1||week>53||first<1||last>12||last<first)throw new IllegalArgumentException();
                initial.from=initial.to=week;initial.day=date.getDayOfWeek().getValue();initial.first=first;initial.last=last;
                initial.start=LocalTime.parse(PeriodGrid.start(first));initial.end=LocalTime.parse(PeriodGrid.end(last));
            }catch(RuntimeException invalid){toast("新增位置无效，请返回重新选择");finish();return;}
            slots.add(initial);
        }
        Button add=new Button(this);add.setText("＋ 添加上课时段");add.setOnClickListener(v->{syncInputs();if(slots.size()>=80){toast("一门课最多80个时段");return;}slots.add(new Slot());renderSlots();});body.addView(add);
        if(!originalIds.isEmpty()){Button delete=new Button(this);delete.setText("删除当前上课时段");delete.setTextColor(getColor(R.color.action_danger));delete.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("删除当前上课时段？").setMessage("只删除所点击时段的安排，其他同名课程时段不变。尚未保存的复制或新增时段也会放弃。").setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->persist(Collections.emptyList())).show());body.addView(delete);}
        TextView note=EditUi.text(this,"仅编辑所点击的上课时段（含其周数范围），不会修改其他同名时段。复制或新增的时段随本次保存。再次获取课表前建议先导出备份。",12,false);body.addView(note);
        if(state!=null&&state.containsKey("draft"))try{org.json.JSONObject draft=new org.json.JSONObject(state.getString("draft"));name.setText(draft.getString("name"));credits.setText(draft.getString("credits"));color=draft.getInt("color");org.json.JSONArray list=draft.getJSONArray("slots");if(list.length()<1||list.length()>80)throw new IllegalArgumentException();slots.clear();for(int n=0;n<list.length();n++){org.json.JSONObject e=list.getJSONObject(n);Slot s=new Slot();s.id=e.getString("id");s.from=e.getInt("from");s.to=e.getInt("to");s.day=e.getInt("day");s.first=e.getInt("first");s.last=e.getInt("last");s.custom=e.getBoolean("custom");s.start=LocalTime.parse(e.getString("start"));s.end=LocalTime.parse(e.getString("end"));s.room=e.getString("room");s.teacher=e.getString("teacher");s.note=e.getString("note");s.type=e.getString("type");slots.add(s);}}catch(Exception invalid){toast("未保存草稿未能恢复，原课表未改变");finish();return;}
        renderSlots();
    }
    private Slot slot(ScheduleRule r){Slot s=new Slot();s.id=r.getId();s.from=r.getFromWeek();s.to=r.getToWeek();s.day=r.getDayOfWeek().getValue();s.start=r.getStartTime();s.end=r.getEndTime();s.room=r.getRoom();s.teacher=r.getTeacher();s.note=r.getNote();s.type=r.getType();int[] span=PeriodGrid.span(r.toSession(ScheduleRepository.today()));s.custom=span==null;if(span!=null){s.first=span[0];s.last=span[1];}return s;}
    private final Map<Slot,EditText[]> inputs=new IdentityHashMap<>();
    private void syncInputs(){for(Map.Entry<Slot,EditText[]> e:inputs.entrySet()){e.getKey().room=e.getValue()[0].getText().toString();e.getKey().teacher=e.getValue()[1].getText().toString();e.getKey().note=e.getValue()[2].getText().toString();}}
    private void renderSlots(){slotContainer.removeAllViews();inputs.clear();
        for(int n=0;n<slots.size();n++){Slot s=slots.get(n);LinearLayout card=EditUi.card(this);slotContainer.addView(card);LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
            TextView label=EditUi.text(this,"时段 "+(n+1),14,true);heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));Button copy=new Button(this);copy.setText("复制");copy.setOnClickListener(v->{syncInputs();if(slots.size()<80){slots.add(s.copy());renderSlots();}else toast("时段已达上限");});heading.addView(copy);
            Button delete=new Button(this);delete.setText("删除");delete.setOnClickListener(v->{syncInputs();if(slots.size()==1){toast("至少保留一个时段，删除当前安排请用底部按钮");return;}slots.remove(s);renderSlots();});heading.addView(delete);card.addView(heading);
            EditUi.row(this,card,"周数","第 "+s.from+"—"+s.to+" 周",()->weekRange(s));
            EditUi.row(this,card,"节数","周"+"一二三四五六日".charAt(s.day-1)+"  第"+s.first+"—"+s.last+"节",()->periods(s));
            EditUi.row(this,card,"自定义时间",s.custom?s.start+"—"+s.end:"关闭",()->{syncInputs();new AlertDialog.Builder(this).setTitle("上课时间").setItems(new String[]{"按学校节次时间","自定义开始与结束"},(d,w)->{if(w==0){s.custom=false;renderSlots();}else customTime(s);}).show();});
            EditText room=EditUi.field(this,card,"教室",s.room,200),teacher=EditUi.field(this,card,"老师",s.teacher,100),note=EditUi.field(this,card,"备注（选填）",s.note,1000);inputs.put(s,new EditText[]{room,teacher,note});
        }
    }
    private void weekRange(Slot s){syncInputs();LinearLayout view=EditUi.card(this);NumberPicker from=number(1,53,s.from),to=number(1,53,s.to);view.addView(EditUi.text(this,"开始周",14,true));view.addView(from);view.addView(EditUi.text(this,"结束周",14,true));view.addView(to);AlertDialog d=new AlertDialog.Builder(this).setTitle("选择周数").setView(view).setNegativeButton("取消",null).setPositiveButton("确定",null).create();d.setOnShowListener(v->d.getButton(-1).setOnClickListener(b->{if(from.getValue()>to.getValue()){toast("结束周不能小于开始周");return;}s.from=from.getValue();s.to=to.getValue();d.dismiss();renderSlots();}));d.show();}
    private void periods(Slot s){syncInputs();LinearLayout view=EditUi.card(this);Spinner day=new Spinner(this);day.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"周一","周二","周三","周四","周五","周六","周日"}));day.setSelection(s.day-1);view.addView(day);LinearLayout row=new LinearLayout(this);NumberPicker from=number(1,12,s.first),to=number(1,12,s.last);row.addView(from,new LinearLayout.LayoutParams(0,-2,1));row.addView(to,new LinearLayout.LayoutParams(0,-2,1));view.addView(row);AlertDialog d=new AlertDialog.Builder(this).setTitle("星期 / 开始节—结束节").setView(view).setNegativeButton("取消",null).setPositiveButton("确定",null).create();d.setOnShowListener(v->d.getButton(-1).setOnClickListener(b->{if(from.getValue()>to.getValue()){toast("结束节不能小于开始节");return;}s.day=day.getSelectedItemPosition()+1;s.first=from.getValue();s.last=to.getValue();s.custom=false;d.dismiss();renderSlots();}));d.show();}
    private NumberPicker number(int from,int to,int current){NumberPicker p=new NumberPicker(this);p.setMinValue(from);p.setMaxValue(to);p.setValue(current);p.setWrapSelectorWheel(false);return p;}
    private void customTime(Slot s){new TimePickerDialog(this,(v,h,m)->{LocalTime start=LocalTime.of(h,m);new TimePickerDialog(this,(v2,h2,m2)->{LocalTime end=LocalTime.of(h2,m2);if(!end.isAfter(start)){toast("结束时间须晚于开始时间");return;}s.start=start;s.end=end;s.custom=true;renderSlots();},s.end.getHour(),s.end.getMinute(),true).show();},s.start.getHour(),s.start.getMinute(),true).show();}
    private void pickColor(){String[] names={"自动","天蓝","樱粉","薄荷","浅紫","杏黄","雾蓝","珊瑚","青绿","玫红"};new AlertDialog.Builder(this).setTitle("课程配色").setItems(names,(d,w)->{color=COLORS[w];colorRow.setText("颜色    "+names[w]+"  ›");toast("已选择"+names[w]+"，保存后生效");}).show();}
    private void save(){syncInputs();try{String credit=credits.getText().toString().trim();if(!credit.isEmpty()&&!credit.matches("[0-9]{1,3}(\\.[0-9]{1,2})?"))throw new IllegalArgumentException("学分请填写有效数字，或留空");List<ScheduleRule> edits=new ArrayList<>();for(Slot s:slots){String start=s.custom?s.start.toString():PeriodGrid.start(s.first),end=s.custom?s.end.toString():PeriodGrid.end(s.last);edits.add(new ScheduleRule(s.from,s.to,DayOfWeek.of(s.day),start,end,s.custom?"自定义时间":"第"+s.first+"—"+s.last+"节",name.getText().toString(),s.room,s.type,s.teacher,s.id,color,credit,s.note));}
        List<ScheduleRule> merged=merged(edits);List<String> conflicts=editConflicts(merged,edits);if(!conflicts.isEmpty())new AlertDialog.Builder(this).setTitle("保存前请确认").setMessage(String.join("\n",conflicts)+"\n\n保留冲突仍可显示，但同一时间可能无法兼顾。").setNegativeButton("返回检查",null).setPositiveButton("仍然保存",(d,w)->persist(edits)).show();else persist(edits);
        }catch(Exception e){toast(e.getMessage()==null?"请检查课程信息":e.getMessage());}}
    private List<ScheduleRule> merged(List<ScheduleRule> edits){List<ScheduleRule> all=ScheduleRuleStore.readRules(this);if(all==null)throw new IllegalArgumentException("原课表读取失败，未覆盖");return SlotEditPolicy.merge(all,originalIds,edits);}
    private List<String> editConflicts(List<ScheduleRule> all,List<ScheduleRule> edits){
        LinkedHashSet<String> warnings=new LinkedHashSet<>(ScheduleConflicts.courses(all));List<AgendaEvent> events=AgendaStore.load(this);
        if(events==null)warnings.add("日程读取失败，本次未能核对与日程的时间冲突");
        else for(AgendaEvent event:events){
            List<CourseSession> lessons=new ArrayList<>();int week=GridPlacementPolicy.week(event.date,ScheduleRepository.schoolStartDate());
            for(ScheduleRule rule:edits)if(rule.appliesTo(event.date,week))lessons.add(rule.toSession(event.date));
            if(!ScheduleConflicts.agenda(event,Collections.emptyList(),lessons).isEmpty())warnings.add("日程冲突："+event.title+" · "+event.date+" "+event.timeText());
            if(warnings.size()>=10)break;
        }
        return new ArrayList<>(warnings);
    }
    private void persist(List<ScheduleRule> edits){try{List<ScheduleRule> updated=merged(edits);ScheduleRuleStore.saveLocal(this,updated);if(!edits.isEmpty()){boolean keepOriginal=false;for(ScheduleRule r:updated)if(r.getCourse().equals(originalName))keepOriginal=true;ReminderPreferences.renameCourse(this,originalName,name.getText().toString(),keepOriginal);}ScheduleChanges.refresh(this);setResult(RESULT_OK);toast("上课时段已保存到本机");finish();}catch(Exception e){toast("保存失败，原课表保留");}}
    private void confirmBack(){new AlertDialog.Builder(this).setTitle("放弃未保存的修改？").setNegativeButton("继续编辑",null).setPositiveButton("放弃",(d,w)->finish()).show();}
    @Override public void onBackPressed(){confirmBack();}
    @Override protected void onSaveInstanceState(Bundle state){syncInputs();try{org.json.JSONArray list=new org.json.JSONArray();for(Slot s:slots)list.put(new org.json.JSONObject().put("id",s.id).put("from",s.from).put("to",s.to).put("day",s.day).put("first",s.first).put("last",s.last).put("custom",s.custom).put("start",s.start.toString()).put("end",s.end.toString()).put("room",s.room).put("teacher",s.teacher).put("note",s.note).put("type",s.type));state.putString("draft",new org.json.JSONObject().put("name",name.getText().toString()).put("credits",credits.getText().toString()).put("color",color).put("slots",list).toString());}catch(Exception ignored){}super.onSaveInstanceState(state);}
    private int dp(int n){return EditUi.dp(this,n);}private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
