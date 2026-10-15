package cn.nua.wenguan.schedule.ui;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import cn.nua.wenguan.schedule.R;
import cn.nua.wenguan.schedule.data.ScheduleRepository;
import cn.nua.wenguan.schedule.model.CourseSession;
import cn.nua.wenguan.schedule.CourseEditorActivity;
import cn.nua.wenguan.schedule.ReminderSettings;
import cn.nua.wenguan.schedule.ReminderPreferences;

/** Shared details for all course views. Explicit edit opens a separate local editor. */
public final class CourseDetailsSheet {
    private CourseDetailsSheet() { }
    public static void show(Context c, CourseSession s) {
        Dialog dialog=new Dialog(c); dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout body=new LinearLayout(c); body.setOrientation(LinearLayout.VERTICAL);
        int pad=Math.round(24*c.getResources().getDisplayMetrics().density); body.setPadding(pad,pad,pad,pad);
        GradientDrawable bg=new GradientDrawable(); bg.setColor(c.getColor(R.color.surface)); bg.setCornerRadius(pad); body.setBackground(bg);
        TextView title=new TextView(c); title.setText(s.getCourse()); title.setTextColor(c.getColor(R.color.text_primary)); title.setTextSize(23); title.setTypeface(null,Typeface.BOLD); body.addView(title);
        TextView details=new TextView(c); details.setText(s.getDate()+"\n\n"+s.getTimeRange()+" · "+s.getPeriods()+"\n\n地点："+s.getRoom()+"\n\n教师："+s.getTeacher()+"\n\n"+s.getType()+" · "+ScheduleRepository.statusAt(s,ScheduleRepository.now()).getLabel());
        details.setTextColor(c.getColor(R.color.text_secondary)); details.setTextSize(16); details.setPadding(0,pad,0,pad); body.addView(details);
        TextView extra=new TextView(c);extra.setText((s.getCredits().isEmpty()?"":"学分："+s.getCredits()+"\n")+(s.getNote().isEmpty()?"":"备注："+s.getNote()+"\n")+"提醒："+ReminderPreferences.label(ReminderPreferences.mode(c,s))+" · 提前"+ReminderPreferences.minutes(c,s)+"分钟");extra.setTextColor(c.getColor(R.color.text_secondary));extra.setTextSize(14);body.addView(extra);
        Button edit=new Button(c);edit.setText("编辑当前上课时段");edit.setOnClickListener(v->{dialog.dismiss();c.startActivity(new android.content.Intent(c,CourseEditorActivity.class).putExtra("rule_id",s.getRuleId()));});body.addView(edit);
        if(c instanceof android.app.Activity){Button reminder=new Button(c);reminder.setText("本次课 / 整门课单独提醒");reminder.setOnClickListener(v->{dialog.dismiss();ReminderSettings.forLesson((android.app.Activity)c,s);});body.addView(reminder);}
        Button close=new Button(c); close.setText("收起详情"); close.setOnClickListener(v->dialog.dismiss()); body.addView(close);
        ScrollView scroll=new ResponsiveScrollView(c); scroll.addView(body); dialog.setContentView(scroll); dialog.setCanceledOnTouchOutside(true);
        Window window=dialog.getWindow(); if(window==null)return;
        UiPerformance.configure(window);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams params=window.getAttributes(); params.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL; params.y=pad/2; params.dimAmount=.28f;
        params.width=c.getResources().getDisplayMetrics().widthPixels-pad; params.height=WindowManager.LayoutParams.WRAP_CONTENT; window.setAttributes(params);
        window.setWindowAnimations(R.style.TopCourseSheetAnimation); dialog.show();
        // Bound tall content without shrinking text or hiding the close action.
        scroll.post(()->{int max=c.getResources().getDisplayMetrics().heightPixels*2/3;if(scroll.getHeight()>max){android.view.ViewGroup.LayoutParams p=scroll.getLayoutParams();p.height=max;scroll.setLayoutParams(p);}});
    }
}
