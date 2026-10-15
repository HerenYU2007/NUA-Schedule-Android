package cn.nua.wenguan.schedule.ui;
import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import cn.nua.wenguan.schedule.*;
import cn.nua.wenguan.schedule.data.*;
import cn.nua.wenguan.schedule.model.*;
import java.time.LocalDate;
import java.util.*;

/** Clickable native course/agenda blocks; a transient drag selection never changes saved data. */
public final class WeekGrid {
    private static final String[] DAYS={"一","二","三","四","五","六","日"};
    private static final int[] LIGHT={0xFF8BCAE8,0xFFF2B7C7,0xFF99D6D1,0xFFC4B4F0,0xFFEAC28B,0xFFAABFEF,0xFFECA891};
    private static final int[] DARK={0xFF275775,0xFF704153,0xFF255D59,0xFF514270,0xFF695333,0xFF394D78,0xFF724838};
    private WeekGrid() { }
    public static View create(Context c,LocalDate selected,List<CourseSession> sessions){
        return create(c,selected,sessions,AgendaStore.load(c),()->{});
    }
    public static View create(Context c,LocalDate selected,List<CourseSession> sessions,List<AgendaEvent> events,Runnable changed){
        int axis=dp(c,48),row=dp(c,82),header=dp(c,56);
        int available=c.getResources().getDisplayMetrics().widthPixels-dp(c,40)-axis;
        int col=Math.max(1,available/7),width=axis+col*7,height=header+row*PeriodGrid.COUNT;
        LocalDate monday=ScheduleRepository.weekMonday(selected);
        LinearLayout body=EditUi.column(c);
        TextView tip=text(c,"空白新增 · 拖框移动 · 拉上下边缘改长度 · 再点编辑",12,c.getColor(R.color.text_secondary));
        tip.setPadding(0,dp(c,6),0,dp(c,8));body.addView(tip);
        LinearLayout tools=new LinearLayout(c);tools.setGravity(Gravity.CENTER_VERTICAL);
        TextView draftStatus=text(c,"左右滑动切周 · 课程与日程可点击查看",12,c.getColor(R.color.text_secondary));draftStatus.setMaxLines(2);
        tools.addView(draftStatus,new LinearLayout.LayoutParams(0,dp(c,48),1));
        Button clear=new Button(c);clear.setText("取消新增");clear.setTextSize(12);clear.setVisibility(View.INVISIBLE);tools.addView(clear);body.addView(tools);
        InteractiveWeekGrid grid=new InteractiveWeekGrid(c,monday,axis,header,col,row,value->{
            draftStatus.setText(value==null?"左右滑动切周 · 课程与日程可点击查看":"未保存 · "+value);
            clear.setVisibility(value==null?View.INVISIBLE:View.VISIBLE);
        });
        clear.setOnClickListener(v->grid.clearDraft());grid.setBackgroundColor(c.getColor(R.color.surface));
        body.addView(grid,new LinearLayout.LayoutParams(width,height));
        add(grid,new WeekGridBackground(c,selected,axis,header,col,row),0,0,width,height);
        List<AgendaEvent> weekEvents=new ArrayList<>();if(events!=null)for(AgendaEvent event:events)if(!event.date.isBefore(monday)&&event.date.isBefore(monday.plusDays(7)))weekEvents.add(event);
        AgendaGridPolicy.Result display=AgendaGridPolicy.plan(sessions,weekEvents);
        List<CourseSession> unsupported=new ArrayList<>();List<AgendaEvent> separate=new ArrayList<>(),overlapping=new ArrayList<>();
        for(int day=0;day<7;day++){
            LocalDate date=monday.plusDays(day);List<Item> daily=new ArrayList<>();
            for(CourseSession s:sessions)if(s.getDate().equals(date)){
                int[] span=PeriodGrid.span(s);if(span==null)unsupported.add(s);else{grid.reserveSpan(day,span[0],span[1]);daily.add(new Item(span[0]-1,span[1],s,null));}
                grid.reserveTime(day,s.getStartTime(),s.getEndTime());
            }
            for(AgendaEvent event:weekEvents)if(event.date.equals(date)){
                if(!event.allDay)grid.reserveTime(day,event.start,event.end);
                float[] span=event.allDay?null:GridPlacementPolicy.agendaSpan(event.start,event.end);
                if(span==null)separate.add(event);else if(display.separateEvents.contains(event.id))overlapping.add(event);else daily.add(new Item(span[0],span[1],null,event));
            }
            daily.sort(Comparator.comparingDouble((Item item)->item.top).thenComparingDouble(item->item.bottom).thenComparing(Item::id));
            for(int first=0;first<daily.size();){
                int end=first+1;float edge=daily.get(first).bottom;
                while(end<daily.size()&&daily.get(end).top<edge-.0001f){edge=Math.max(edge,daily.get(end).bottom);end++;}
                int lanes=end-first;
                if(lanes>1){
                    List<Item> choices=new ArrayList<>(daily.subList(first,end));Item primary=choices.get(0);
                    TextView cluster=text(c,"重叠"+lanes+"项\n"+(primary.course!=null?primary.course.getCourse():primary.event.title)+"\n点开选择",12,c.getColor(R.color.text_primary));
                    cluster.setPadding(dp(c,4),dp(c,5),dp(c,3),dp(c,3));cluster.setBackground(EditUi.shape(c,c.getColor(R.color.nav_background),7));
                    cluster.setContentDescription("该时段重叠"+lanes+"项，点击逐项查看");cluster.setOnClickListener(v->chooseItems(c,choices,changed));
                    add(grid,cluster,axis+day*col+dp(c,2),header+(int)(primary.top*row)+dp(c,3),Math.max(1,col-dp(c,4)),Math.max(dp(c,18),(int)((edge-primary.top)*row)-dp(c,6)));
                    first=end;continue;
                }
                for(int n=first;n<end;n++){
                    Item item=daily.get(n);CourseSession s=item.course;AgendaEvent event=item.event;
                    String title=s!=null?s.getCourse()+"\n\n@"+s.getRoom():"日程"+(event.done?" ✓":"")+"\n"+event.title+"\n"+event.timeText()+"\n@"+event.place;
                    TextView block=text(c,title,12,c.getColor(R.color.text_primary));block.setGravity(Gravity.TOP|Gravity.START);block.setPadding(dp(c,4),dp(c,5),dp(c,3),dp(c,3));block.setTypeface(null,Typeface.BOLD);block.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    int blockHeight=Math.max(dp(c,18),(int)((item.bottom-item.top)*row)-dp(c,6));block.setMaxLines(Math.max(1,(int)((blockHeight-dp(c,6))/(14*c.getResources().getDisplayMetrics().scaledDensity))));
                    int color;
                    if(s!=null){int index=Math.floorMod(s.getCourse().hashCode(),LIGHT.length);color=s.getColor()!=0?s.getColor():(AppearanceSettings.dark(c)?DARK:LIGHT)[index];}
                    else color=AppearanceSettings.dark(c)?0xFF5E4A26:event.done?0xFFE8E2D6:0xFFFFD997;
                    double brightness=(Color.red(color)*.2126+Color.green(color)*.7152+Color.blue(color)*.0722)/255;
                    block.setTextColor(brightness>.55?0xFF142A3C:Color.WHITE);
                    GradientDrawable bg=EditUi.shape(c,color,7);bg.setStroke(dp(c,1),c.getColor(event==null?R.color.card_border:R.color.action_amber));block.setBackground(bg);
                    block.setContentDescription(s!=null?details(s):"日程："+event.title+"，"+event.date+"，"+event.timeText()+"，"+event.place+(event.done?"，已完成":""));
                    block.setOnClickListener(v->{if(s!=null)CourseDetailsSheet.show(c,s);else editEvent(c,event,changed);});
                    float laneWidth=(float)col/lanes;int inset=Math.min(dp(c,2),Math.max(0,(int)laneWidth/4));
                    int x=axis+day*col+(int)((n-first)*laneWidth)+inset,y=header+(int)(item.top*row)+dp(c,3),blockWidth=Math.max(1,(int)laneWidth-2*inset);
                    add(grid,block,x,y,blockWidth,blockHeight);
                    if(s!=null&&display.markedCourses.contains(s.getRuleId())){
                        TextView badge=text(c,"!",11,c.getColor(R.color.action_amber));badge.setGravity(Gravity.CENTER);badge.setTypeface(null,Typeface.BOLD);badge.setBackground(EditUi.shape(c,c.getColor(R.color.surface),5));badge.setContentDescription("当天有重叠日程，点击查看");
                        badge.setOnClickListener(v->chooseAgenda(c,weekEvents,display.separateEvents,s,changed));add(grid,badge,x+Math.max(0,blockWidth-dp(c,16)),y,Math.min(blockWidth,dp(c,16)),dp(c,18));
                    }
                }
                first=end;
            }
        }
        if(events==null)body.addView(text(c,"日程读取失败，原数据保留。",14,c.getColor(R.color.action_danger)));
        if(!overlapping.isEmpty()){
            TextView label=text(c,"重叠日程（"+overlapping.size()+"）",16,c.getColor(R.color.text_primary));label.setPadding(0,dp(c,18),0,dp(c,8));body.addView(label);
            body.addView(text(c,"这些日程单独列出，避免把课程挤成细线；课程上的 ! 可查看当天重叠日程。",12,c.getColor(R.color.text_secondary)));
            Map<String,AgendaConflictIndex.Report> reports=AgendaConflictIndex.build(weekEvents,date->{List<CourseSession> result=new ArrayList<>();for(CourseSession session:sessions)if(session.getDate().equals(date))result.add(session);return result;});
            overlapping.sort(Comparator.comparing((AgendaEvent event)->event.date).thenComparing(event->event.start));
            for(AgendaEvent event:overlapping){
                AgendaConflictIndex.Report report=reports.get(event.id);String detail=report==null?"为避免卡片重叠，单独显示":report.description();
                TextView card=text(c,"日程"+(event.done?" · 已完成":"")+"  "+event.title+"\n"+event.date+" · "+event.timeText()+"\n"+detail,14,c.getColor(R.color.text_primary));card.setPadding(dp(c,12),dp(c,12),dp(c,12),dp(c,12));card.setBackground(EditUi.shape(c,c.getColor(R.color.card_practice),12));card.setOnClickListener(v->editEvent(c,event,changed));body.addView(card);
            }
        }
        if(!separate.isEmpty()){
            TextView label=text(c,"全天 / 课间及非上课时间的日程",16,c.getColor(R.color.text_primary));label.setPadding(0,dp(c,18),0,dp(c,8));body.addView(label);
            separate.sort(Comparator.comparing((AgendaEvent e)->e.date).thenComparing(e->e.start));
            for(AgendaEvent event:separate){TextView card=text(c,"日程"+(event.done?" · 已完成":"")+"  "+event.title+"\n"+event.date+" · "+event.timeText()+"\n"+event.place,14,c.getColor(R.color.text_primary));card.setPadding(dp(c,12),dp(c,12),dp(c,12),dp(c,12));card.setBackground(EditUi.shape(c,c.getColor(R.color.card_practice),12));card.setOnClickListener(v->editEvent(c,event,changed));body.addView(card);}
        }
        if(!unsupported.isEmpty()){
            body.addView(text(c,"以下课程的自定义时间不对应标准节次，单独列出避免错位：",12,c.getColor(R.color.text_secondary)));
            for(CourseSession s:unsupported){TextView item=text(c,details(s),14,c.getColor(R.color.text_primary));item.setOnClickListener(v->CourseDetailsSheet.show(c,s));body.addView(item);}
        }
        return body;
    }
    private static final class Item {
        final float top,bottom;final CourseSession course;final AgendaEvent event;
        Item(float top,float bottom,CourseSession course,AgendaEvent event){this.top=top;this.bottom=bottom;this.course=course;this.event=event;}
        String id(){return course==null?"agenda:"+event.id:"course:"+course.getRuleId();}
    }
    private static void chooseItems(Context c,List<Item> items,Runnable changed){
        String[] labels=new String[items.size()];for(int n=0;n<items.size();n++){Item item=items.get(n);labels[n]=item.course!=null?"课程："+item.course.getCourse()+" · "+item.course.getTimeRange():"日程："+item.event.title+" · "+item.event.timeText();}
        new android.app.AlertDialog.Builder(c).setTitle("选择重叠安排").setItems(labels,(dialog,which)->{Item item=items.get(which);if(item.course!=null)CourseDetailsSheet.show(c,item.course);else editEvent(c,item.event,changed);}).setNegativeButton("关闭",null).show();
    }
    private static void chooseAgenda(Context c,List<AgendaEvent> events,Set<String> overlapping,CourseSession course,Runnable changed){
        List<AgendaEvent> choices=new ArrayList<>();for(AgendaEvent event:events)if(event.date.equals(course.getDate())&&!event.allDay&&(overlapping.contains(event.id)||(event.start.isBefore(course.getEndTime())&&course.getStartTime().isBefore(event.end))))choices.add(event);
        String[] labels=new String[choices.size()];for(int n=0;n<choices.size();n++)labels[n]=choices.get(n).title+" · "+choices.get(n).timeText();
        new android.app.AlertDialog.Builder(c).setTitle("当天重叠日程").setItems(labels,(dialog,which)->editEvent(c,choices.get(which),changed)).setNegativeButton("关闭",null).show();
    }
    private static void editEvent(Context c,AgendaEvent event,Runnable changed){if(c instanceof Activity)AgendaEditor.show((Activity)c,event,event.date,changed);}
    private static String details(CourseSession s){return s.getCourse()+"\n"+s.getDate()+"\n"+s.getTimeRange()+" · "+s.getPeriods()+"\n地点："+s.getRoom()+"\n教师："+s.getTeacher()+"\n"+s.getType()+" · "+ScheduleRepository.statusAt(s,ScheduleRepository.now()).getLabel();}
    private static TextView text(Context c,String value,int size,int color){TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);return t;}
    private static void add(FrameLayout parent,View view,int x,int y,int width,int height){FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(width,height);p.leftMargin=x;p.topMargin=y;parent.addView(view,p);}
    private static int dp(Context c,int value){return EditUi.dp(c,value);}
}
