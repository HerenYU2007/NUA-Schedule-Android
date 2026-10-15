package cn.nua.wenguan.schedule.ui;
import android.app.*;
import android.widget.*;
import cn.nua.wenguan.schedule.NotificationAccess;
import cn.nua.wenguan.schedule.data.*;
import cn.nua.wenguan.schedule.model.AgendaEvent;
import java.time.*;
import java.util.*;
public final class AgendaEditor {
    private AgendaEditor() { }
    public static void show(Activity a,AgendaEvent old,LocalDate selected,Runnable changed){
        LocalDate[] date={old==null?selected:old.date};LocalTime[] start={old==null?LocalTime.of(9,0):old.start},end={old==null?LocalTime.of(10,0):old.end};
        LinearLayout form=EditUi.column(a),basic=EditUi.card(a);form.addView(basic);
        EditText title=EditUi.field(a,basic,"日程标题（必填）",old==null?"":old.title,80),place=EditUi.field(a,basic,"地点（选填）",old==null?"":old.place,200);
        final TextView[] day={null},from={null},to={null};
        day[0]=EditUi.row(a,basic,"日期",date[0].toString(),()->new DatePickerDialog(a,(view,y,m,d)->{date[0]=LocalDate.of(y,m+1,d);day[0].setText("日期    "+date[0]+"  ›");},date[0].getYear(),date[0].getMonthValue()-1,date[0].getDayOfMonth()).show());
        CheckBox all=new CheckBox(a);all.setText("全天日程");all.setChecked(old!=null&&old.allDay);basic.addView(all);
        from[0]=EditUi.row(a,basic,"开始时间",start[0].toString(),()->new TimePickerDialog(a,(v,h,m)->{start[0]=LocalTime.of(h,m);from[0].setText("开始时间    "+start[0]+"  ›");},start[0].getHour(),start[0].getMinute(),true).show());
        to[0]=EditUi.row(a,basic,"结束时间",end[0].toString(),()->new TimePickerDialog(a,(v,h,m)->{end[0]=LocalTime.of(h,m);to[0].setText("结束时间    "+end[0]+"  ›");},end[0].getHour(),end[0].getMinute(),true).show());
        from[0].setEnabled(!all.isChecked());to[0].setEnabled(!all.isChecked());all.setOnCheckedChangeListener((b,value)->{from[0].setEnabled(!value);to[0].setEnabled(!value);});
        LinearLayout options=EditUi.card(a);form.addView(options);CheckBox remind=new CheckBox(a);remind.setText("开启日程提醒");remind.setChecked(old!=null&&old.remind);options.addView(remind);options.addView(EditUi.text(a,"提前15分钟；全天日程为当天09:00。通知或准时提醒权限未允许时，可能无法及时提醒。",13,false));
        final Dialog[] dialog={null};
        Runnable[] save={null};save[0]=()->{try{
            AgendaEvent event=new AgendaEvent(old==null?UUID.randomUUID().toString():old.id,title.getText().toString(),place.getText().toString(),date[0],start[0],end[0],all.isChecked(),old!=null&&old.done,remind.isChecked());
            java.util.List<AgendaEvent> events=AgendaStore.load(a);if(events==null)throw new IllegalArgumentException("原日程读取失败，未覆盖");
            Runnable commit=()->{if(!AgendaStore.put(a,event,false)){Toast.makeText(a,"保存失败，原日程保留",Toast.LENGTH_LONG).show();return;}dialog[0].dismiss();changed.run();if(event.remind&&!NotificationAccess.allowed(a))NotificationAccess.request(a);};
            java.util.List<String> conflicts=ScheduleConflicts.agenda(event,events,ScheduleRepository.sessionsOn(event.date));
            if(conflicts.isEmpty())commit.run();else new AlertDialog.Builder(a).setTitle("这个时段已有安排").setMessage(String.join("、",conflicts)+"\n仍要保存吗？").setNegativeButton("返回修改",null).setPositiveButton("仍然保存",(d,w)->commit.run()).show();
        }catch(IllegalArgumentException error){Toast.makeText(a,error.getMessage(),Toast.LENGTH_LONG).show();}};
        if(old!=null){Button delete=new Button(a);delete.setText("删除日程");delete.setOnClickListener(v->new AlertDialog.Builder(a).setTitle("删除这条日程？").setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->{if(AgendaStore.put(a,old,true)){dialog[0].dismiss();changed.run();}else Toast.makeText(a,"删除失败，原日程保留",Toast.LENGTH_LONG).show();}).show());form.addView(delete);}
        dialog[0]=EditUi.sheet(a,old==null?"添加日程":"编辑日程",form,"保存",save[0]);
    }
}
