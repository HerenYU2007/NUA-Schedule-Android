package cn.nua.wenguan.schedule;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.*;
import cn.nua.wenguan.schedule.data.ScheduleRepository;
import cn.nua.wenguan.schedule.model.*;
import cn.nua.wenguan.schedule.ui.EditUi;
import cn.nua.wenguan.schedule.ui.ReminderChoiceGroup;
import java.util.*;
public final class ReminderSettings {
    private ReminderSettings() { }
    private static final int[] MINUTES={0,5,10,15,20,30,60,120};
    private static Spinner minutes(Activity a,int chosen){Spinner s=new Spinner(a);String[] labels=new String[MINUTES.length];for(int n=0;n<labels.length;n++)labels[n]=MINUTES[n]==0?"上课时":"提前"+MINUTES[n]+"分钟";s.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,labels));for(int n=0;n<MINUTES.length;n++)if(MINUTES[n]==chosen)s.setSelection(n);return s;}
    private static Spinner modes(Activity a,String[] names,int selected){Spinner s=new Spinner(a);s.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,names));s.setSelection(selected);return s;}
    public static void show(Activity a){
        NotificationAccess.channels(a);LinearLayout body=EditUi.column(a);
        CheckBox enabled=new CheckBox(a);enabled.setText("开启上课提醒");enabled.setChecked(ReminderScheduler.enabled(a));body.addView(enabled);
        LinearLayout card=EditUi.card(a);body.addView(card);
        card.addView(EditUi.text(a,"提醒方式",14,true));Spinner mode=modes(a,new String[]{"通知提醒","闹钟响铃（最多60秒，可停止）"},ReminderPreferences.defaultMode(a)==2?1:0);card.addView(mode);
        card.addView(EditUi.text(a,"提前时间",14,true));Spinner before=minutes(a,ReminderScheduler.minutesBefore(a));card.addView(before);
        CheckBox only=new CheckBox(a);only.setText("只提醒所选课程");only.setChecked(ReminderPreferences.selectedOnly(a));card.addView(only);
        Set<String> selected=ReminderPreferences.selected(a);
        TreeSet<String> names=new TreeSet<>();for(ScheduleRule r:ScheduleRepository.rules())names.add(r.getCourse());
        EditUi.row(a,card,"提醒课程","已选"+selected.size()+"门",()->{
            String[] choices=names.toArray(new String[0]);boolean[] checks=new boolean[choices.length];for(int n=0;n<choices.length;n++)checks[n]=selected.contains(choices[n]);
            new AlertDialog.Builder(a).setTitle("选择需要提醒的课程").setMultiChoiceItems(choices,checks,(d,w,value)->{checks[w]=value;}).setNegativeButton("取消",null).setPositiveButton("确定",(d,w)->{selected.clear();for(int n=0;n<choices.length;n++)if(checks[n])selected.add(choices[n]);Toast.makeText(a,"已选择"+selected.size()+"门，点保存生效",Toast.LENGTH_SHORT).show();}).show();
        });
        LinearLayout access=EditUi.card(a);body.addView(access);access.addView(EditUi.text(a,"通知："+(NotificationAccess.allowed(a)?"已允许":"未允许")+"\n准时闹钟："+(ReminderScheduler.exactAllowed(a)?"已允许":"未允许")+"\n选择某一节课的详情，还可覆盖本次或整门课的提醒。\n闹钟遵循系统闹钟音量与勿扰规则；未授权准时闹钟时降级为通知。强制停止仍会影响提醒。",13,false));
        EditUi.row(a,access,"通知权限","打开系统设置",()->NotificationAccess.request(a));
        EditUi.row(a,access,"准时提醒权限","打开系统设置",()->exact(a));
        EditUi.row(a,access,"测试通知","立即发送测试，不修改课程",()->{if(!NotificationAccess.post(a,NotificationAccess.CLASSES,7230,"测试提醒","看到这条通知，说明通知发送通道可用。锁屏/响铃表现仍需单独测试。",false))NotificationAccess.request(a);});
        EditUi.row(a,access,"测试闹钟","确认后响铃5秒",()->new AlertDialog.Builder(a).setTitle("现在测试响铃？").setMessage("使用系统闹钟音量，请先注意周围环境。").setNegativeButton("取消",null).setPositiveButton("测试",(d,w)->{if(!NotificationAccess.allowed(a)){NotificationAccess.request(a);return;}try{a.startForegroundService(new Intent(a,CourseAlarmService.class).putExtra("title","测试闹钟").putExtra("body","将在5秒后停止").putExtra("duration",5000));}catch(RuntimeException e){Toast.makeText(a,"系统未允许启动响铃",Toast.LENGTH_LONG).show();}}).show());
        final Dialog[] dialog={null};dialog[0]=EditUi.sheet(a,"上课提醒",body,"保存",()->{
            if(only.isChecked()&&selected.isEmpty()&&enabled.isChecked()){Toast.makeText(a,"请至少选择一门课程",Toast.LENGTH_LONG).show();return;}
            if(!ReminderPreferences.configure(a,enabled.isChecked(),MINUTES[before.getSelectedItemPosition()],mode.getSelectedItemPosition()+1,only.isChecked(),selected)){Toast.makeText(a,"保存失败，原设置保留",Toast.LENGTH_LONG).show();return;}
            dialog[0].dismiss();ReminderScheduler.scheduleNext(a);Toast.makeText(a,"提醒设置已保存",Toast.LENGTH_SHORT).show();if(enabled.isChecked()&&!NotificationAccess.allowed(a))NotificationAccess.request(a);else if(enabled.isChecked()&&mode.getSelectedItemPosition()==1&&!ReminderScheduler.exactAllowed(a))exact(a);
        });
    }
    public static void forLesson(Activity a,CourseSession s){
        NotificationAccess.channels(a);
        LinearLayout body=EditUi.column(a),hero=EditUi.card(a);body.addView(hero);
        TextView badge=EditUi.text(a,"课程提醒",12,true);badge.setTextColor(a.getColor(R.color.brand_navy));hero.addView(badge);
        TextView course=EditUi.text(a,s.getCourse(),22,true);course.setPadding(0,EditUi.dp(a,8),0,EditUi.dp(a,8));hero.addView(course);
        TextView meta=EditUi.text(a,s.getDate()+" 周"+"一二三四五六日".charAt(s.getDate().getDayOfWeek().getValue()-1)+" · "+s.getTimeRange()+"\n"+s.getRoom(),13,false);meta.setTextColor(a.getColor(R.color.text_secondary));hero.addView(meta);
        section(a,body,"应用范围");
        ReminderChoiceGroup scope=new ReminderChoiceGroup(a,new String[]{"仅本次","整门课程"},new String[]{"这一天的这次课","所有同名上课时段"},true);body.addView(scope);
        TextView scopeHint=EditUi.text(a,"",12,false);scopeHint.setTextColor(a.getColor(R.color.text_secondary));scopeHint.setPadding(0,EditUi.dp(a,8),0,EditUi.dp(a,16));body.addView(scopeHint);
        section(a,body,"提醒方式");
        ReminderChoiceGroup mode=new ReminderChoiceGroup(a,new String[]{"跟随默认设置","不提醒","通知提醒","闹钟响铃"},
                new String[]{"使用整门课或全局设置","关闭所选范围的提醒","在通知栏显示课程与地点","使用系统闹钟音量，最多响铃60秒"},false);body.addView(mode);
        int[] draftMode={ReminderPreferences.overrideMode(a,s,true),ReminderPreferences.overrideMode(a,s,false)};
        int[] draftMinutes={ReminderPreferences.overrideMinutes(a,s,true),ReminderPreferences.overrideMinutes(a,s,false)};
        LinearLayout time=EditUi.card(a);body.addView(time);
        final TextView[] before={null};
        Runnable refreshTime=()->{int index=scope.value();time.setVisibility(draftMode[index]>0?android.view.View.VISIBLE:android.view.View.GONE);before[0].setText("提前时间    "+(draftMinutes[index]==0?"上课时提醒":"提前 "+draftMinutes[index]+" 分钟")+"  ›");};
        before[0]=EditUi.row(a,time,"提前时间","",()->{
            String[] choices=new String[MINUTES.length+1];for(int i=0;i<MINUTES.length;i++)choices[i]=MINUTES[i]==0?"上课时提醒":"提前 "+MINUTES[i]+" 分钟";choices[MINUTES.length]="自定义提前时间";
            int index=scope.value(),checked=MINUTES.length;for(int i=0;i<MINUTES.length;i++)if(draftMinutes[index]==MINUTES[i])checked=i;
            new AlertDialog.Builder(a).setTitle("什么时候提醒").setSingleChoiceItems(choices,checked,(picker,which)->{
                picker.dismiss();
                if(which<MINUTES.length){draftMinutes[index]=MINUTES[which];refreshTime.run();}
                else{NumberPicker number=new NumberPicker(a);number.setMinValue(0);number.setMaxValue(120);number.setValue(draftMinutes[index]);number.setWrapSelectorWheel(false);
                    new AlertDialog.Builder(a).setTitle("提前分钟数（0—120）").setView(number).setNegativeButton("取消",null).setPositiveButton("确定",(d,w)->{number.clearFocus();draftMinutes[index]=number.getValue();refreshTime.run();}).show();}
            }).setNegativeButton("取消",null).show();
        });
        Runnable bind=()->{int index=scope.value();mode.setValue(Math.max(0,Math.min(3,draftMode[index]+1)));scopeHint.setText(index==0?"只影响 "+s.getDate()+" 这一次上课，其他日期不变。":"影响所有同名课程；已单独设置的某次提醒仍优先。");refreshTime.run();};
        scope.setOnChoice(index->bind.run());mode.setOnChoice(index->{draftMode[scope.value()]=index-1;refreshTime.run();});bind.run();
        LinearLayout access=EditUi.card(a);body.addView(access);
        TextView accessStatus=EditUi.text(a,"",13,false);access.addView(accessStatus);
        access.addOnAttachStateChangeListener(new android.view.View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(android.view.View v){accessStatus.setText("提醒总开关："+(ReminderScheduler.enabled(a)?"已开启":"未开启")+"  ·  通知："+(NotificationAccess.allowed(a)?"已允许":"未允许")+"\n本页不会自动开启总开关；未授权准时提醒时降级为通知。");}public void onViewDetachedFromWindow(android.view.View v){}});
        EditUi.row(a,access,"系统权限","查看或授权",()->new AlertDialog.Builder(a).setTitle("提醒权限").setItems(new String[]{"通知权限："+(NotificationAccess.allowed(a)?"已允许":"未允许"),"准时提醒："+(ReminderScheduler.exactAllowed(a)?"已允许":"未允许")},(d,w)->{if(w==0)NotificationAccess.request(a);else exact(a);}).show());
        final Dialog[] dialog={null};dialog[0]=EditUi.sheet(a,"单独提醒",body,"保存",()->{
            int index=scope.value(),value=draftMode[index];
            if(!ReminderPreferences.override(a,s,index==0,value,draftMinutes[index])){Toast.makeText(a,"保存失败，请重试",Toast.LENGTH_SHORT).show();return;}
            dialog[0].dismiss();ScheduleChanges.refreshAsync(a);Toast.makeText(a,"已保存："+(index==0?"仅本次 · ":"整门课程 · ")+ReminderPreferences.label(value),Toast.LENGTH_LONG).show();
            if(value>0&&!ReminderScheduler.enabled(a))new AlertDialog.Builder(a).setTitle("还需开启提醒总开关").setMessage("单独设置已保存，总开关当前关闭。开启后才会提醒，不会擅自开启全部课程。").setNegativeButton("暂不开启",null).setPositiveButton("去设置",(d,w)->show(a)).show();
            else if(value>0&&!NotificationAccess.allowed(a))NotificationAccess.request(a);else if(value==2&&!ReminderScheduler.exactAllowed(a))exact(a);
        });
    }
    private static void section(Activity a,LinearLayout body,String title){TextView label=EditUi.text(a,title,14,true);label.setPadding(0,0,0,EditUi.dp(a,10));body.addView(label);}
    public static void exact(Activity a){try{if(Build.VERSION.SDK_INT>=31)a.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+a.getPackageName())));}catch(RuntimeException denied){Toast.makeText(a,"请到系统的闹钟与提醒权限中授权",Toast.LENGTH_LONG).show();}}
}
