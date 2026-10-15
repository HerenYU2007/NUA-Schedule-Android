package cn.nua.wenguan.schedule;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.widget.*;
import cn.nua.wenguan.schedule.data.*;
/** Edits a draft; cancel/back never writes preferences. */
public final class PeriodTimeSettings {
    private PeriodTimeSettings(){}
    public static void show(SettingsActivity activity){
        try{PeriodTimeStore.load(activity);}catch(RuntimeException e){Toast.makeText(activity,"节次时间读取失败，原设置保留",Toast.LENGTH_LONG).show();return;}
        final String[][] draft={SchoolPeriodTimes.starts(),SchoolPeriodTimes.ends()};
        LinearLayout rows=new LinearLayout(activity);rows.setOrientation(LinearLayout.VERTICAL);
        int pad=(int)(16*activity.getResources().getDisplayMetrics().density);rows.setPadding(pad,pad,pad,pad);
        TextView hint=new TextView(activity);hint.setText("点时间修改，每节结束必须晚于开始，节次之间不能重叠。保存后统一用于课程、提醒和小组件；自定义时间的课程不变。");rows.addView(hint);
        Button[] labels=new Button[12];
        for(int p=1;p<=12;p++){
            final int period=p;Button row=new Button(activity);labels[p-1]=row;row.setAllCaps(false);
            row.setText(label(period,draft));rows.addView(row);
            row.setOnClickListener(v->new AlertDialog.Builder(activity).setTitle("第"+period+"节")
                .setItems(new String[]{"开始 "+draft[0][period],"结束 "+draft[1][period]},(d,which)->{
                    String[] parts=draft[which][period].split(":");
                    new TimePickerDialog(activity,(view,h,m)->{
                        draft[which][period]=String.format(java.util.Locale.ROOT,"%02d:%02d",h,m);row.setText(label(period,draft));
                    },Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),true).show();
                }).show());
        }
        ScrollView scroll=new ScrollView(activity);scroll.addView(rows);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("全局节次时间").setView(scroll)
            .setNegativeButton("取消",null).setNeutralButton("恢复默认",null).setPositiveButton("保存",null).create();
        dialog.setOnShowListener(ignored->{
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->new AlertDialog.Builder(activity)
                .setTitle("恢复默认时间？").setMessage("仅重置当前草稿，点击保存后才生效。").setNegativeButton("取消",null)
                .setPositiveButton("恢复",(d,w)->{draft[0]=SchoolPeriodTimes.defaultStarts();draft[1]=SchoolPeriodTimes.defaultEnds();for(int p=1;p<=12;p++)labels[p-1].setText(label(p,draft));}).show());
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                try{
                    if(!PeriodTimeStore.save(activity,draft[0],draft[1])){Toast.makeText(activity,"保存失败，原设置保留",Toast.LENGTH_LONG).show();return;}
                    ScheduleRuleStore.applySaved(activity);ScheduleChanges.refreshAsync(activity);dialog.dismiss();
                    Toast.makeText(activity,"节次时间已保存",Toast.LENGTH_SHORT).show();
                }catch(RuntimeException e){Toast.makeText(activity,e.getMessage(),Toast.LENGTH_LONG).show();}
            });
        });dialog.show();
    }
    private static String label(int p,String[][] draft){return "第"+p+"节    "+draft[0][p]+" — "+draft[1][p];}
}
