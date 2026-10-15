package cn.nua.wenguan.schedule.ui;
import android.app.*;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import cn.nua.wenguan.schedule.R;

/** Shared rounded editor primitives; headers/actions stay visible above the keyboard. */
public final class EditUi {
    private EditUi() { }
    public static int dp(Context c, float n) { return Math.round(n*c.getResources().getDisplayMetrics().density); }
    public static GradientDrawable shape(Context c, int color, int radius) { GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d; }
    public static TextView text(Context c,String s,int size,boolean bold) { TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(c.getColor(R.color.text_primary));if(bold)t.setTypeface(null,Typeface.BOLD);return t; }
    public static LinearLayout column(Context c) { LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l; }
    public static LinearLayout card(Context c) { LinearLayout l=column(c);l.setPadding(dp(c,16),dp(c,8),dp(c,16),dp(c,8));l.setBackground(shape(c,c.getColor(R.color.surface),22));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(c,16);l.setLayoutParams(p);return l; }
    public static EditText field(Context c,LinearLayout card,String label,String value,int limit) {
        TextView caption=text(c,label,12,false);caption.setTextColor(c.getColor(R.color.text_secondary));caption.setPadding(0,dp(c,10),0,0);card.addView(caption);
        EditText input=new EditText(c);input.setText(value);input.setHint(label);input.setTextSize(16);input.setTextColor(c.getColor(R.color.text_primary));input.setSingleLine(true);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(limit)});card.addView(input,new LinearLayout.LayoutParams(-1,dp(c,52)));return input;
    }
    public static TextView row(Context c,LinearLayout card,String label,String value,Runnable click) {
        TextView row=text(c,label+"    "+value+(click==null?"":"  ›"),16,false);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinHeight(dp(c,56));row.setPadding(0,dp(c,8),0,dp(c,8));card.addView(row,new LinearLayout.LayoutParams(-1,-2));
        if(click!=null){row.setClickable(true);row.setFocusable(true);row.setOnClickListener(v->click.run());}
        View line=new View(c);line.setBackgroundColor(c.getColor(R.color.card_border));card.addView(line,new LinearLayout.LayoutParams(-1,dp(c,1)));return row;
    }
    public static Dialog sheet(Activity a,String name,View body,String positive,Runnable save) {
        Dialog dialog=new Dialog(a);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root=column(a);root.setPadding(dp(a,18),dp(a,12),dp(a,18),dp(a,16));root.setBackground(shape(a,a.getColor(R.color.page_background),24));
        LinearLayout header=new LinearLayout(a);header.setGravity(Gravity.CENTER_VERTICAL);
        Button cancel=new Button(a);cancel.setText("取消");cancel.setOnClickListener(v->dialog.dismiss());header.addView(cancel);
        TextView title=text(a,name,20,true);title.setGravity(Gravity.CENTER);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button done=new Button(a);done.setText(positive);done.setOnClickListener(v->save.run());header.addView(done);root.addView(header);
        ScrollView scroll=new ResponsiveScrollView(a);scroll.setFillViewport(false);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));dialog.setContentView(root);
        Window w=dialog.getWindow();UiPerformance.configure(w);if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);w.setWindowAnimations(R.style.TopCourseSheetAnimation);w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}
        dialog.setCanceledOnTouchOutside(false);dialog.show();if(w!=null)w.setLayout(a.getResources().getDisplayMetrics().widthPixels-dp(a,20),a.getResources().getDisplayMetrics().heightPixels*4/5);return dialog;
    }
}
