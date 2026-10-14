package cn.nua.wenguan.schedule.ui;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Build;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import cn.nua.wenguan.schedule.data.AgendaStore;
import cn.nua.wenguan.schedule.model.AgendaEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public final class AgendaEditor {
    private AgendaEditor() { }
    public static void show(Activity a,AgendaEvent old,LocalDate selected,Runnable changed){
        LocalDate[] date={old==null?selected:old.date};LocalTime[] start={old==null?LocalTime.of(9,0):old.start},end={old==null?LocalTime.of(10,0):old.end};
        LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);int pad=Math.round(20*a.getResources().getDisplayMetrics().density);form.setPadding(pad,pad,pad,pad);
        EditText title=new EditText(a);title.setHint("日程标题（必填）");title.setSingleLine(true);title.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});title.setText(old==null?"":old.title);form.addView(title);
        EditText place=new EditText(a);place.setHint("地点（可选）");place.setSingleLine(true);place.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(200)});place.setText(old==null?"":old.place);form.addView(place);
        Button day=new Button(a);day.setText("日期："+date[0]);day.setOnClickListener(v->new DatePickerDialog(a,(view,y,m,d)->{date[0]=LocalDate.of(y,m+1,d);day.setText("日期："+date[0]);},date[0].getYear(),date[0].getMonthValue()-1,date[0].getDayOfMonth()).show());form.addView(day);
        CheckBox all=new CheckBox(a);all.setText("全天日程");all.setChecked(old!=null&&old.allDay);form.addView(all);
        Button from=new Button(a),to=new Button(a);from.setText("开始："+start[0]);to.setText("结束："+end[0]);form.addView(from);form.addView(to);
        from.setOnClickListener(v->new TimePickerDialog(a,(view,h,m)->{start[0]=LocalTime.of(h,m);from.setText("开始："+start[0]);},start[0].getHour(),start[0].getMinute(),true).show());
        to.setOnClickListener(v->new TimePickerDialog(a,(view,h,m)->{end[0]=LocalTime.of(h,m);to.setText("结束："+end[0]);},end[0].getHour(),end[0].getMinute(),true).show());
        from.setEnabled(!all.isChecked());to.setEnabled(!all.isChecked());all.setOnCheckedChangeListener((b,value)->{from.setEnabled(!value);to.setEnabled(!value);});
        CheckBox remind=new CheckBox(a);remind.setText("提醒：提前15分钟（全天日程为当日09:00）\n系统省电或未授权可能延迟/阻止通知");remind.setChecked(old!=null&&old.remind);form.addView(remind);
        ScrollView scroll=new ScrollView(a);scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(old==null?"添加日程":"编辑日程").setView(scroll).setNegativeButton("取消",null).setPositiveButton("保存",null).setNeutralButton(old==null?"":"删除",old==null?null:(d,w)->new AlertDialog.Builder(a).setTitle("删除这条日程？").setNegativeButton("取消",null).setPositiveButton("删除",(confirm,which)->{if(AgendaStore.put(a,old,true))changed.run();else Toast.makeText(a,"删除失败，原数据保留",Toast.LENGTH_LONG).show();}).show()).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{
            AgendaEvent event=new AgendaEvent(old==null?UUID.randomUUID().toString():old.id,title.getText().toString(),place.getText().toString(),date[0],start[0],end[0],all.isChecked(),old!=null&&old.done,remind.isChecked());
            if(!AgendaStore.put(a,event,false))throw new IllegalArgumentException("日程保存失败，原数据保留");
            dialog.dismiss();changed.run();
            if(event.remind&&Build.VERSION.SDK_INT>=33&&a.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)a.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},8429);
        }catch(IllegalArgumentException error){Toast.makeText(a,error.getMessage(),Toast.LENGTH_LONG).show();}}));dialog.show();
        if(old==null)dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(android.view.View.GONE);
    }
}
