package cn.nua.wenguan.schedule.ui;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.TextView;
import cn.nua.wenguan.schedule.R;

/** Distinct native action cards with responsive text, themed colors and accessible button roles. */
public final class MineActionCards {
    private MineActionCards(){}
    public static View create(Context context,int icon,String title,String description,String badge,int accent,boolean primary,Runnable action){
        View card=LayoutInflater.from(context).inflate(R.layout.view_mine_action,null,false);
        int tone=context.getColor(accent),foreground=context.getColor(primary?R.color.on_button_primary:R.color.text_primary);
        int detail=context.getColor(primary?R.color.on_button_primary:R.color.text_secondary);
        GradientDrawable background=shape(context,primary?context.getColor(R.color.button_primary):context.getColor(R.color.surface),22);
        if(!primary)background.setStroke(dp(context,1),context.getColor(R.color.card_border));
        card.setBackground(new RippleDrawable(ColorStateList.valueOf(context.getColor(R.color.control_ripple)),background,shape(context,0xffffffff,22)));
        ImageView image=card.findViewById(R.id.mine_action_icon);image.setImageResource(icon);
        image.setImageTintList(ColorStateList.valueOf(primary?foreground:tone));
        image.setBackground(shape(context,((primary?foreground:tone)&0x00ffffff)|0x18000000,14));
        TextView name=card.findViewById(R.id.mine_action_title);name.setText(title);name.setTextColor(foreground);
        TextView subtitle=card.findViewById(R.id.mine_action_description);subtitle.setText(description);subtitle.setTextColor(detail);
        if(primary)subtitle.setAlpha(.88f);
        TextView tag=card.findViewById(R.id.mine_action_badge);tag.setText(badge);tag.setTextColor(primary?foreground:tone);tag.setTypeface(null,Typeface.BOLD);
        tag.setBackground(shape(context,((primary?foreground:tone)&0x00ffffff)|0x18000000,6));
        ((ImageView)card.findViewById(R.id.mine_action_arrow)).setImageTintList(ColorStateList.valueOf(primary?foreground:context.getColor(R.color.text_hint)));
        android.widget.LinearLayout.LayoutParams params=new android.widget.LinearLayout.LayoutParams(-1,-2);params.bottomMargin=dp(context,12);card.setLayoutParams(params);
        card.setContentDescription(title+"，"+description+"，"+badge);
        card.setAccessibilityDelegate(new View.AccessibilityDelegate(){
            @Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName(android.widget.Button.class.getName());}
        });
        card.setOnClickListener(v->action.run());return card;
    }
    private static GradientDrawable shape(Context c,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
    private static int dp(Context c,int value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
}
