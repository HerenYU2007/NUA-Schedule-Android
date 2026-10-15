package cn.nua.wenguan.schedule.ui;
import android.app.Activity;
import android.content.*;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import cn.nua.wenguan.schedule.*;
import cn.nua.wenguan.schedule.data.ScheduleRepository;
import java.time.LocalDate;
import java.util.function.Consumer;

/** One blank hit layer and one transient draft, not 84 additional clickable cell views. */
public final class InteractiveWeekGrid extends FrameLayout {
    private final int axis,header,column,row;
    private final LocalDate monday;
    private final Consumer<String> status;
    private DraftBlock draft;
    private final GridOccupancyPolicy occupancy=new GridOccupancyPolicy();
    public void reserveSpan(int day,int first,int last){occupancy.reserve(day,first,last);}
    public void reserveTime(int day,java.time.LocalTime start,java.time.LocalTime end){occupancy.reserveTime(day,start,end);}
    private boolean suppressBlankTap;
    public boolean hasDraft(){return draft!=null;}
    public boolean draftContains(float rawX,float rawY){android.graphics.Rect bounds=new android.graphics.Rect();return draft!=null&&draft.getGlobalVisibleRect(bounds)&&bounds.contains((int)rawX,(int)rawY);}
    public void cancelForOutsideTap(){suppressBlankTap=true;clearDraft();}
    public void finishOutsideTap(){suppressBlankTap=false;}
    public static InteractiveWeekGrid findDraft(View root){
        if(root instanceof InteractiveWeekGrid&&((InteractiveWeekGrid)root).hasDraft())return (InteractiveWeekGrid)root;
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int n=0;n<group.getChildCount();n++){InteractiveWeekGrid found=findDraft(group.getChildAt(n));if(found!=null)return found;}}
        return null;
    }
    public InteractiveWeekGrid(Context c,LocalDate monday,int axis,int header,int column,int row,Consumer<String> status){
        super(c);this.monday=monday;this.axis=axis;this.header=header;this.column=column;this.row=row;this.status=status;
        View blank=new View(c){
            private float downX,downY;private boolean cancelled;
            @Override public boolean onTouchEvent(MotionEvent e){
                if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();cancelled=false;return GridPlacementPolicy.hit(downX,downY,axis,header,column,row)!=null;}
                if(e.getPointerCount()>1)cancelled=true;
                if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){cancelled=true;return true;}
                if(e.getActionMasked()==MotionEvent.ACTION_UP){
                    int slop=ViewConfiguration.get(c).getScaledTouchSlop();
                    if(!cancelled&&Math.hypot(e.getX()-downX,e.getY()-downY)<=slop)performClick();
                }
                return true;
            }
            @Override public boolean performClick(){super.performClick();int[] hit=GridPlacementPolicy.hit(downX,downY,axis,header,column,row);if(hit!=null)select(hit[0],hit[1]);return true;}
        };
        blank.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(blank,new LayoutParams(-1,-1));
    }
    public void clearDraft(){if(draft!=null){draft.stop();removeView(draft);draft=null;}status.accept(null);}
    private void select(int day,int first){
        if(suppressBlankTap)return;
        if(draft!=null){clearDraft();return;}
        if(!occupancy.free(day,first,first)){Toast.makeText(getContext(),"这个时段已有课程或日程，请点击已有安排查看",Toast.LENGTH_SHORT).show();return;}
        int week=GridPlacementPolicy.week(monday.plusDays(day),ScheduleRepository.schoolStartDate());
        if(week<1||week>53){Toast.makeText(getContext(),"该日期不在开学后第1—53周，请先核对开学时间",Toast.LENGTH_LONG).show();return;}
        clearDraft();draft=new DraftBlock(day,first,first);addView(draft);draft.place();
    }
    private final class DraftBlock extends FrameLayout {
        private final int day;private int first,last,initialFirst,initialLast,kind,initialScroll;
        private float downX,downY,lastY;private boolean moved,dragging,cancelled;
        private long lastFrame;
        private final TextView label;
        private ResponsiveScrollView scroller;
        private final int[] screen=new int[2];
        DraftBlock(int day,int first,int last){
            super(InteractiveWeekGrid.this.getContext());this.day=day;this.first=first;this.last=last;
            Context c=getContext();setClickable(true);setFocusable(true);
            GradientDrawable bg=EditUi.shape(c,c.getColor(R.color.nav_background),8);bg.setStroke(dp(2),c.getColor(R.color.brand_navy));setBackground(bg);
            label=EditUi.text(c,"新增课\n点击编辑",12,true);label.setGravity(Gravity.CENTER);label.setTextColor(c.getColor(R.color.brand_navy));
            LayoutParams text=new LayoutParams(-1,-1);text.topMargin=dp(16);text.bottomMargin=dp(16);addView(label,text);
            for(int edge:new int[]{Gravity.TOP,Gravity.BOTTOM}){TextView handle=EditUi.text(c,"━",14,true);handle.setTextColor(c.getColor(R.color.brand_navy));handle.setGravity(Gravity.CENTER);LayoutParams p=new LayoutParams(-1,dp(18),edge);addView(handle,p);}
            setAccessibilityDelegate(new View.AccessibilityDelegate(){
                @Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);}
                @Override public boolean performAccessibilityAction(View host,int action,android.os.Bundle args){
                    if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD||action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD){int[] span=GridPlacementPolicy.drag(DraftBlock.this.first,DraftBlock.this.last,action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD?1:-1,GridPlacementPolicy.MOVE);update(span);return true;}return super.performAccessibilityAction(host,action,args);
                }
            });
        }
        private int dp(int value){return EditUi.dp(getContext(),value);}
        private ResponsiveScrollView findScroller(){for(ViewParent p=getParent();p!=null;p=p.getParent())if(p instanceof ResponsiveScrollView)return (ResponsiveScrollView)p;return null;}
        private void place(){
            LayoutParams p=new LayoutParams(Math.max(1,column-dp(4)),(last-first+1)*row-dp(6));p.leftMargin=axis+day*column+dp(2);p.topMargin=header+(first-1)*row+dp(3);setLayoutParams(p);
            LocalDate date=monday.plusDays(day);
            String value=date+" · 第"+first+"—"+last+"节 · "+PeriodGrid.start(first)+"—"+PeriodGrid.end(last);
            setContentDescription("未保存的新课程，"+value+"。上下拖动移动，拖上下边缘调整长度，点击详细编辑。");status.accept(value);
        }
        private void update(int[] span){if(!occupancy.free(day,span[0],span[1]))return;if(first==span[0]&&last==span[1])return;first=span[0];last=span[1];place();}
        private void follow(){int scroll=scroller==null?0:scroller.getScrollY()-initialScroll;int delta=Math.round((lastY-downY+scroll)/row);update(GridPlacementPolicy.drag(initialFirst,initialLast,delta,kind));}
        private final Runnable autoScroll=new Runnable(){@Override public void run(){
            if(!dragging||scroller==null)return;
            scroller.getLocationOnScreen(screen);float edge=dp(48),top=lastY-screen[1],bottom=screen[1]+scroller.getHeight()-lastY;
            int direction=top<edge?-1:bottom<edge?1:0;
            long now=SystemClock.uptimeMillis(),elapsed=lastFrame==0?16:Math.min(32,now-lastFrame);lastFrame=now;
            if(direction!=0){scroller.scrollBy(0,direction*Math.max(1,(int)(dp(300)*elapsed/1000f)));follow();}
            postOnAnimation(this);
        }};
        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){
                downX=e.getRawX();downY=lastY=e.getRawY();initialFirst=first;initialLast=last;scroller=findScroller();initialScroll=scroller==null?0:scroller.getScrollY();moved=dragging=cancelled=false;
                kind=e.getY()<dp(18)?GridPlacementPolicy.START:e.getY()>getHeight()-dp(18)?GridPlacementPolicy.END:GridPlacementPolicy.MOVE;
                getParent().requestDisallowInterceptTouchEvent(true);return true;
            }
            if(e.getPointerCount()>1||e.getActionMasked()==MotionEvent.ACTION_CANCEL){cancelled=true;update(new int[]{initialFirst,initialLast});stop();return true;}
            if(cancelled){stop();return true;}
            if(e.getActionMasked()==MotionEvent.ACTION_MOVE){
                lastY=e.getRawY();int slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();
                if(Math.hypot(e.getRawX()-downX,lastY-downY)>slop)moved=true;
                if(!dragging&&Math.abs(lastY-downY)>slop){dragging=true;lastFrame=0;postOnAnimation(autoScroll);}
                if(dragging)follow();return true;
            }
            if(e.getActionMasked()==MotionEvent.ACTION_UP){boolean click=!moved;stop();if(click)performClick();return true;}return true;
        }
        private void stop(){dragging=false;removeCallbacks(autoScroll);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}
        @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
        @Override public boolean performClick(){
            super.performClick();Context c=getContext();
            c.startActivity(new Intent(c,CourseEditorActivity.class).putExtra("draft_date",monday.plusDays(day).toEpochDay()).putExtra("draft_first",first).putExtra("draft_last",last));
            return true;
        }
    }
}
