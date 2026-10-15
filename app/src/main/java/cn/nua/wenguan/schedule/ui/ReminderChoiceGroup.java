package cn.nua.wenguan.schedule.ui;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import cn.nua.wenguan.schedule.R;
import java.util.*;
import java.util.function.IntConsumer;

/** Accessible, native, compact selection cards instead of unlabeled platform spinners. */
public final class ReminderChoiceGroup extends LinearLayout {
    private final List<LinearLayout> rows=new ArrayList<>();
    private final List<TextView> marks=new ArrayList<>(),titles=new ArrayList<>();
    private int selected;
    private IntConsumer listener;
    public ReminderChoiceGroup(Context c,String[] names,String[] descriptions,boolean compact){
        super(c);setOrientation(compact?HORIZONTAL:VERTICAL);
        for(int i=0;i<names.length;i++){
            final int index=i;
            LinearLayout row=new LinearLayout(c);row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(EditUi.dp(c,12),EditUi.dp(c,12),EditUi.dp(c,12),EditUi.dp(c,12));
            row.setMinimumHeight(EditUi.dp(c,compact?52:70));
            LinearLayout label=EditUi.column(c);
            TextView title=EditUi.text(c,names[i],15,true);label.addView(title);
            if(!descriptions[i].isEmpty()){
                TextView detail=EditUi.text(c,descriptions[i],12,false);detail.setTextColor(c.getColor(R.color.text_secondary));detail.setPadding(0,EditUi.dp(c,4),0,0);label.addView(detail);
            }
            row.addView(label,new LayoutParams(0,-2,1));
            TextView mark=EditUi.text(c,"",18,true);mark.setGravity(Gravity.CENTER);
            row.addView(mark,new LayoutParams(EditUi.dp(c,24),-2));
            LayoutParams p=new LayoutParams(compact?0:-1,-2,compact?1:0);
            if(compact&&i>0)p.leftMargin=EditUi.dp(c,8);else if(!compact)p.bottomMargin=EditUi.dp(c,8);
            addView(row,p);rows.add(row);marks.add(mark);titles.add(title);
            row.setClickable(true);row.setFocusable(true);row.setContentDescription(names[i]+(descriptions[i].isEmpty()?"":"，"+descriptions[i]));
            row.setAccessibilityDelegate(new View.AccessibilityDelegate(){
                @Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info){
                    super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName(RadioButton.class.getName());info.setCheckable(true);info.setChecked(selected==index);
                }
            });
            row.setOnClickListener(v->{setValue(index);if(listener!=null)listener.accept(index);});
        }
        setValue(0);
    }
    public void setOnChoice(IntConsumer callback){listener=callback;}
    public int value(){return selected;}
    public void setValue(int value){
        if(value<0||value>=rows.size())throw new IllegalArgumentException("Unsupported reminder choice");
        selected=value;Context c=getContext();
        for(int i=0;i<rows.size();i++){
            boolean checked=i==selected;
            GradientDrawable bg=EditUi.shape(c,c.getColor(checked?R.color.nav_background:R.color.surface),16);
            bg.setStroke(EditUi.dp(c,1),c.getColor(checked?R.color.brand_navy:R.color.card_border));
            rows.get(i).setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(c.getColor(R.color.control_ripple)),bg,new ColorDrawable(android.graphics.Color.WHITE)));
            rows.get(i).setSelected(checked);marks.get(i).setText(checked?"✓":"○");
            marks.get(i).setTextColor(c.getColor(checked?R.color.brand_navy:R.color.text_hint));
            titles.get(i).setTextColor(c.getColor(checked?R.color.brand_navy:R.color.text_primary));
        }
    }
}
