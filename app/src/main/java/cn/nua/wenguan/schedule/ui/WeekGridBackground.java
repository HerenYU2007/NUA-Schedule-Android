package cn.nua.wenguan.schedule.ui;

import android.content.Context;
import android.graphics.*;
import android.text.*;
import android.view.View;
import cn.nua.wenguan.schedule.R;
import cn.nua.wenguan.schedule.data.ScheduleRepository;
import java.time.LocalDate;

/** One retained display list replaces 34 repeated date/time/line views per week. */
final class WeekGridBackground extends View {
    private final int axis,header,column,row,lineColor,selectedColor,todayColor;
    private final int todayDay,selectedDay;
    private final StaticLayout month;
    private final StaticLayout[] days=new StaticLayout[7],periods=new StaticLayout[12];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    WeekGridBackground(Context c,LocalDate selected,int axis,int header,int column,int row){
        super(c);this.axis=axis;this.header=header;this.column=column;this.row=row;
        lineColor=c.getColor(R.color.card_border);selectedColor=c.getColor(R.color.brand_navy);todayColor=c.getColor(R.color.nav_background);
        LocalDate monday=ScheduleRepository.weekMonday(selected),today=ScheduleRepository.today();selectedDay=selected.getDayOfWeek().getValue()-1;todayDay=!today.isBefore(monday)&&today.isBefore(monday.plusDays(7))?today.getDayOfWeek().getValue()-1:-1;
        month=text(c,selected.getMonthValue()+"月\n节次",12,c.getColor(R.color.text_secondary),false,axis,false);
        StringBuilder accessible=new StringBuilder("周课表日期与节次，").append(monday).append("至").append(monday.plusDays(6));
        for(int day=0;day<7;day++)days[day]=text(c,"一二三四五六日".charAt(day)+"\n"+monday.plusDays(day).getDayOfMonth(),13,day==todayDay?selectedColor:c.getColor(R.color.text_primary),day==todayDay,column,true);
        for(int period=1;period<=12;period++){periods[period-1]=text(c,period+"\n"+PeriodGrid.start(period)+"\n"+PeriodGrid.end(period),11,c.getColor(R.color.text_secondary),false,axis,true);accessible.append("；第").append(period).append("节 ").append(PeriodGrid.start(period)).append('—').append(PeriodGrid.end(period));}
        setContentDescription(accessible);setClickable(false);
    }
    private static StaticLayout text(Context c,String value,int sp,int color,boolean bold,int width,boolean centered){
        TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);paint.setTextSize(sp*c.getResources().getDisplayMetrics().scaledDensity);paint.setColor(color);paint.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);
        return StaticLayout.Builder.obtain(value,0,value.length(),paint,Math.max(1,width)).setIncludePad(false).setAlignment(centered?Layout.Alignment.ALIGN_CENTER:Layout.Alignment.ALIGN_NORMAL).build();
    }
    private void draw(Canvas canvas,StaticLayout layout,float x,float y){canvas.save();canvas.translate(x,y);layout.draw(canvas);canvas.restore();}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float density=getResources().getDisplayMetrics().density;
        if(todayDay>=0){paint.setColor(todayColor);canvas.drawRect(axis+todayDay*column,0,axis+(todayDay+1)*column,header,paint);}
        paint.setColor(selectedColor);canvas.drawRect(axis+selectedDay*column+3*density,header-3*density,axis+(selectedDay+1)*column-3*density,header-density,paint);
        draw(canvas,month,0,0);
        for(int day=0;day<7;day++)draw(canvas,days[day],axis+day*column,Math.max(0,(header-days[day].getHeight())/2f));
        paint.setColor(lineColor);paint.setStrokeWidth(Math.max(1,density));
        for(int period=0;period<12;period++){draw(canvas,periods[period],0,header+period*row+6*density);canvas.drawLine(axis,header+period*row,column*7+axis,header+period*row,paint);}
    }
}
