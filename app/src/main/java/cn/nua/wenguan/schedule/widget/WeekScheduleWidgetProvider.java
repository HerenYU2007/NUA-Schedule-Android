package cn.nua.wenguan.schedule.widget;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.widget.RemoteViews;
import cn.nua.wenguan.schedule.*;
import cn.nua.wenguan.schedule.data.*;
import cn.nua.wenguan.schedule.model.*;
import cn.nua.wenguan.schedule.ui.PeriodGrid;
import java.time.*;
import java.util.*;

/** Independent weekly vertical widget. Bitmap is bounded to stay below IPC bitmap limits. */
public class WeekScheduleWidgetProvider extends AppWidgetProvider {
    private static final String PREVIOUS="cn.nua.wenguan.schedule.widget.WEEK_PREVIOUS",NEXT="cn.nua.wenguan.schedule.widget.WEEK_NEXT",TODAY="cn.nua.wenguan.schedule.widget.WEEK_TODAY";
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){refreshProvider(c,getClass());WidgetUpdateScheduler.scheduleNext(c,ScheduleRepository.sessionsOn(ScheduleRepository.today()),ScheduleRepository.now());}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){ScheduleRuleStore.applySaved(c);updateSafely(c,m,id,options);}
    @Override public void onDeleted(Context c,int[] ids){android.content.SharedPreferences.Editor e=c.getSharedPreferences("week_widget",0).edit();for(int id:ids)e.remove("week_"+id);e.apply();}
    @Override public void onReceive(Context c,Intent i){
        PendingResult pending=goAsync();Context app=c.getApplicationContext();Intent event=new Intent(i);
        if(WidgetWork.sharedRefresh(event)){WidgetWork.enqueue(app,"all-widgets",()->WidgetRecovery.refreshNow(app),pending::finish);return;}
        WidgetWork.enqueue(app,WidgetWork.eventKey(getClass(),event),()->receiveOnWorker(app,event),pending::finish);
    }
    private void receiveOnWorker(Context c,Intent i){super.onReceive(c,i);String action=i.getAction();if(PREVIOUS.equals(action)||NEXT.equals(action)||TODAY.equals(action)){
        int id=i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);AppWidgetManager m=AppWidgetManager.getInstance(c);if(id<0||m.getAppWidgetInfo(id)==null)return;
        long current=c.getSharedPreferences("week_widget",0).getLong("week_"+id,ScheduleRepository.weekMonday(ScheduleRepository.today()).toEpochDay());
        long next=TODAY.equals(action)?ScheduleRepository.weekMonday(ScheduleRepository.today()).toEpochDay():Math.max(ScheduleRepository.today().minusYears(2).toEpochDay(),Math.min(ScheduleRepository.today().plusYears(3).toEpochDay(),current+(NEXT.equals(action)?7:-7)));
        android.content.SharedPreferences.Editor e=c.getSharedPreferences("week_widget",0).edit();if(TODAY.equals(action))e.remove("week_"+id);else e.putLong("week_"+id,next);e.apply();ScheduleRuleStore.applySaved(c);updateSafely(c,m,id,m.getAppWidgetOptions(id));
    }else if(Intent.ACTION_BOOT_COMPLETED.equals(action)||Intent.ACTION_DATE_CHANGED.equals(action)||Intent.ACTION_TIMEZONE_CHANGED.equals(action)||Intent.ACTION_TIME_CHANGED.equals(action)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(action))refreshProvider(c,getClass());}
    public static void refreshAll(Context c){refreshProvider(c,WeekScheduleWidgetProvider.class);}
    protected static void refreshProvider(Context c,Class<? extends WeekScheduleWidgetProvider> type){ScheduleRuleStore.applySaved(c);AppWidgetManager m=AppWidgetManager.getInstance(c);for(int id:m.getAppWidgetIds(new ComponentName(c,type)))updateSafely(c,m,id,m.getAppWidgetOptions(id));}
    @Override public void onDisabled(Context c){if(!WidgetRecovery.hasWidgets(c)){WidgetUpdateScheduler.cancel(c);WidgetRecovery.cancel(c);}}
    @Override public void onRestored(Context c,int[] oldIds,int[] newIds){
        android.content.SharedPreferences prefs=c.getSharedPreferences("week_widget",0);android.content.SharedPreferences.Editor edit=prefs.edit();AppWidgetManager manager=AppWidgetManager.getInstance(c);
        for(int n=0;n<Math.min(oldIds.length,newIds.length);n++){
            if(oldIds[n]!=newIds[n]){if(prefs.contains("week_"+oldIds[n]))edit.putLong("week_"+newIds[n],prefs.getLong("week_"+oldIds[n],0));edit.remove("week_"+oldIds[n]);}
            Bundle options=new Bundle();options.putBoolean(AppWidgetManager.OPTION_APPWIDGET_RESTORE_COMPLETED,true);manager.updateAppWidgetOptions(newIds[n],options);
        }
        edit.apply();WidgetRecovery.begin(c,null);
    }
    private static LocalDate shownMonday(Context c,int id){
        try{LocalDate value=LocalDate.ofEpochDay(c.getSharedPreferences("week_widget",0).getLong("week_"+id,ScheduleRepository.weekMonday(ScheduleRepository.today()).toEpochDay()));if(value.getYear()>=1900&&value.getYear()<=2200)return value;}catch(RuntimeException invalid){}
        return ScheduleRepository.weekMonday(ScheduleRepository.today());
    }
    private static void updateSafely(Context c,AppWidgetManager manager,int id,Bundle options){
        if(options==null)options=new Bundle();
        try{update(c,manager,id,options);WidgetDiagnostics.submitted(c,"week",id);}catch(RuntimeException failed){
            WidgetDiagnostics.failure(c,"week-layout",id,failed);
            try{int[] w=WidgetSizePolicy.range(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,0),options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,0),320),h=WidgetSizePolicy.range(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,0),options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,0),480);long budget=WeekBitmapPolicy.totalBudget(c.getResources().getDisplayMetrics().widthPixels,c.getResources().getDisplayMetrics().heightPixels);manager.updateAppWidget(id,views(c,id,shownMonday(c,id),w[0],h[1],budget/4));WidgetDiagnostics.submitted(c,"week-fallback",id);}
            catch(RuntimeException unavailable){WidgetDiagnostics.failure(c,"week-fallback",id,unavailable);}
        }
    }
    private static void update(Context c,AppWidgetManager manager,int id,Bundle options){
        LocalDate monday=shownMonday(c,id);
        android.util.DisplayMetrics metrics=c.getResources().getDisplayMetrics();
        long budget=WeekBitmapPolicy.totalBudget(metrics.widthPixels,metrics.heightPixels);
        if(WidgetHostPolicy.singleLayout(c.getResources().getConfiguration().smallestScreenWidthDp)){
            int[] size=WidgetHostPolicy.currentSize(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,0),options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,0),options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,0),options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,0),320,480,c.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE);
            manager.updateAppWidget(id,views(c,id,monday,size[0],size[1],budget));return;
        }
        if(android.os.Build.VERSION.SDK_INT>=31){
            ArrayList<android.util.SizeF> sizes=options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
            java.util.LinkedHashSet<android.util.SizeF> accepted=new java.util.LinkedHashSet<>();
            if(sizes!=null)for(android.util.SizeF size:sizes)if(size!=null&&WidgetSizePolicy.current(size.getWidth(),size.getHeight(),options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,0),options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,0),options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,0),options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,0))){accepted.add(size);if(accepted.size()>=4)break;}
            if(!accepted.isEmpty()){
                Map<android.util.SizeF,RemoteViews> variants=new LinkedHashMap<>();long each=budget/accepted.size();
                for(android.util.SizeF size:accepted)variants.put(size,views(c,id,monday,(int)size.getWidth(),(int)size.getHeight(),each));
                manager.updateAppWidget(id,new RemoteViews(variants));return;
            }
        }
        int[] widths=WidgetSizePolicy.range(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,0),options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,0),320),heights=WidgetSizePolicy.range(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,0),options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,0),480);
        manager.updateAppWidget(id,new RemoteViews(views(c,id,monday,widths[1],heights[0],budget/2),views(c,id,monday,widths[0],heights[1],budget/2)));
    }
    private static RemoteViews views(Context c,int id,LocalDate monday,int hostWidth,int hostHeight,long bitmapBudget){
        AppWidgetProviderInfo info=AppWidgetManager.getInstance(c).getAppWidgetInfo(id);boolean white=info!=null&&info.provider.getClassName().equals(WhiteWeekScheduleWidgetProvider.class.getName());
        int week=ScheduleRepository.weekNumber(monday),width=Math.max(150,Math.min(800,hostWidth-16)),height=Math.max(100,Math.min(1000,hostHeight-90));
        RemoteViews views=new RemoteViews(c.getPackageName(),WidgetLayoutEpoch.layout(c,white?R.layout.widget_week_white:R.layout.widget_week));views.setTextViewText(R.id.week_widget_title,(week>0?"第"+week+"周 · ":"")+monday.getMonthValue()+"/"+monday.getDayOfMonth()+"—"+monday.plusDays(6).getMonthValue()+"/"+monday.plusDays(6).getDayOfMonth());
        views.setImageViewBitmap(R.id.week_widget_image,draw(c,monday,width,height,bitmapBudget,white));views.setContentDescription(R.id.week_widget_image,"周课表，"+monday+"至"+monday.plusDays(6)+"，点击在应用中查看完整课程");
        Intent open=new Intent(c,MainActivity.class).putExtra("selected_date",monday.toEpochDay()).putExtra("show_grid",true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending=PendingIntent.getActivity(c,id+50000,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);views.setOnClickPendingIntent(R.id.week_widget_image,pending);
        String[] actions={PREVIOUS,TODAY,NEXT};int[] buttons={R.id.week_widget_previous,R.id.week_widget_today,R.id.week_widget_next};
        for(int n=0;n<3;n++){Intent event=new Intent(c,white?WhiteWeekScheduleWidgetProvider.class:WeekScheduleWidgetProvider.class).setAction(actions[n]).addFlags(Intent.FLAG_RECEIVER_FOREGROUND).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);views.setOnClickPendingIntent(buttons[n],PendingIntent.getBroadcast(c,id*7+n,event,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));}
        return views;
    }
    private static Bitmap draw(Context c,LocalDate monday,int width,int height,long bitmapBudget,boolean white){
        int[] pixels=WeekBitmapPolicy.pixels(width,height,c.getResources().getDisplayMetrics().density,bitmapBudget);
        Bitmap b=Bitmap.createBitmap(pixels[0],pixels[1],Bitmap.Config.ARGB_8888);
        b.setDensity(Bitmap.DENSITY_NONE);
        Canvas canvas=new Canvas(b);canvas.scale((float)pixels[0]/width,(float)pixels[1]/height);canvas.drawColor(white?Color.WHITE:0xFF172C3B);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);float axis=32,head=30,row=(height-head-16)/12f,col=(width-axis)/7f;String[] days={"一","二","三","四","五","六","日"};
        for(int n=0;n<7;n++){LocalDate date=monday.plusDays(n);p.setColor(white?(date.equals(ScheduleRepository.today())?0xFF255A84:0xFF213547):(date.equals(ScheduleRepository.today())?0xFF9ED6FF:Color.WHITE));p.setTextSize(10);canvas.drawText(days[n]+" "+date.getDayOfMonth(),axis+n*col+2,15,p);}
        for(int n=0;n<12;n++){p.setColor(white?0xFF536B7B:0xFF9DB6C7);p.setTextSize(9);canvas.drawText(String.valueOf(n+1),3,head+n*row+12,p);if(row>=24){p.setTextSize(7);canvas.drawText(PeriodGrid.start(n+1),2,head+n*row+23,p);}p.setColor(white?0xFFDCE6EF:0xFF304653);canvas.drawLine(axis,head+n*row,width,head+n*row,p);}
        int custom=0;
        for(int day=0;day<7;day++){
            java.util.List<CourseSession> sessions=ScheduleRepository.sessionsOn(monday.plusDays(day));java.util.List<CourseSession> valid=new ArrayList<>();for(CourseSession s:sessions){if(PeriodGrid.span(s)==null)custom++;else valid.add(s);}
            // Match connected overlap groups in the app rather than painting over collisions.
            for(int first=0;first<valid.size();){int last=first+1,edge=PeriodGrid.span(valid.get(first))[1];while(last<valid.size()&&PeriodGrid.span(valid.get(last))[0]<=edge){edge=Math.max(edge,PeriodGrid.span(valid.get(last))[1]);last++;}int lanes=last-first;
                for(int n=first;n<last;n++){CourseSession s=valid.get(n);int[] span=PeriodGrid.span(s);float lane=col/lanes,x=axis+day*col+(n-first)*lane+1,y=head+(span[0]-1)*row+1,h=(span[1]-span[0]+1)*row-2;int[] colors={0xFF8BCAE8,0xFFF2B7C7,0xFF99D6D1,0xFFC4B4F0,0xFFEAC28B,0xFFAABFEF};p.setColor(s.getColor()!=0?s.getColor():colors[Math.floorMod(s.getCourse().hashCode(),colors.length)]);canvas.drawRoundRect(x,y,x+lane-2,y+h,4,4,p);canvas.save();canvas.clipRect(x+1,y+1,x+lane-3,y+h-1);p.setColor(luminance(p.getColor())>.45?0xFF142A3C:Color.WHITE);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextSize(Math.max(9,Math.min(13,(lane-6)/2.15f)));String value=s.getCourse()+"\n@"+s.getRoom();wrap(canvas,p,value,x+3,y+13,lane-6,h-4);p.setTypeface(Typeface.DEFAULT);canvas.restore();}
                first=last;
            }
        }
        if(!ScheduleRepository.hasImportedRules()){p.setColor(white?0xFF213547:Color.WHITE);p.setTextSize(12);canvas.drawText("点击打开应用，先导入课表",10,height/2f,p);}else {p.setColor(white?0xFF536B7B:Color.WHITE);p.setTextSize(8);canvas.drawText(custom>0?"另有"+custom+"个自定义时段，点击查看":"点击查看完整课表 · 可拉大组件",axis,height-3,p);}return b;
    }
    private static void wrap(Canvas c,Paint p,String value,float x,float y,float width,float height){if(width<4||height<3)return;android.text.TextPaint paint=new android.text.TextPaint(p);int lines=Math.max(1,(int)(height/(paint.getFontMetrics().descent-paint.getFontMetrics().ascent+2)));android.text.StaticLayout layout=android.text.StaticLayout.Builder.obtain(value,0,value.length(),paint,(int)width).setIncludePad(false).setLineSpacing(2,1).setMaxLines(lines).setEllipsize(android.text.TextUtils.TruncateAt.END).build();c.save();c.translate(x,y-p.getTextSize());layout.draw(c);c.restore();}
    private static double luminance(int color){return (Color.red(color)*.2126+Color.green(color)*.7152+Color.blue(color)*.0722)/255;}
}
