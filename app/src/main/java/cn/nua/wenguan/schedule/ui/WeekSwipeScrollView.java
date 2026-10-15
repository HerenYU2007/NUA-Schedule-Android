package cn.nua.wenguan.schedule.ui;
import android.content.Context;
import android.util.AttributeSet;
import android.view.*;
import java.util.function.IntConsumer;
import java.util.function.DoubleConsumer;

/** Raw-coordinate paging keeps finger tracking stable when the viewport itself translates. */
public final class WeekSwipeScrollView extends ResponsiveScrollView {
    private float x,y;private boolean horizontal,vertical,cancelled;private VelocityTracker velocity;
    private IntConsumer onWeek;private DoubleConsumer onDrag;
    public WeekSwipeScrollView(Context c,AttributeSet attrs){super(c,attrs);}
    public void setOnWeekSwipe(IntConsumer callback){onWeek=callback;}
    public void setOnWeekDrag(DoubleConsumer callback){onDrag=callback;}
    private void release(){if(velocity!=null){velocity.recycle();velocity=null;}}
    @Override public boolean dispatchTouchEvent(MotionEvent e){
        if(onWeek==null)return super.dispatchTouchEvent(e);
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){release();velocity=VelocityTracker.obtain();x=e.getRawX();y=e.getRawY();horizontal=vertical=cancelled=false;}
        if(e.getPointerCount()>1)cancelled=true;
        if(velocity!=null){MotionEvent raw=MotionEvent.obtain(e);raw.offsetLocation(e.getRawX()-e.getX(),e.getRawY()-e.getY());velocity.addMovement(raw);raw.recycle();}
        try{return super.dispatchTouchEvent(e);}
        finally{if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)release();}
    }
    private void decide(MotionEvent e){
        if(horizontal||vertical||cancelled)return;
        int decision=WeekSwipePolicy.lock(e.getRawX()-x,e.getRawY()-y,ViewConfiguration.get(getContext()).getScaledTouchSlop());
        horizontal=decision==1;vertical=decision==2;
        if(horizontal){MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);super.onTouchEvent(cancel);cancel.recycle();}
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent e){
        if(onWeek==null)return super.onInterceptTouchEvent(e);
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&!cancelled){
            decide(e);if(horizontal)return true;
            if(!vertical)return false; // Wait for intent instead of locking on the first few noisy pixels.
        }
        return super.onInterceptTouchEvent(e);
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        if(onWeek==null)return super.onTouchEvent(e);
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE)decide(e);
        if(horizontal){
            int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_MOVE&&!cancelled&&onDrag!=null)onDrag.accept(e.getRawX()-x);
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
                float speed=0;if(velocity!=null){velocity.computeCurrentVelocity(1000,ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity());speed=velocity.getXVelocity();}
                int direction=cancelled||action==MotionEvent.ACTION_CANCEL?0:WeekSwipePolicy.directionWithVelocity(e.getRawX()-x,e.getRawY()-y,speed,getResources().getDisplayMetrics().density);
                horizontal=false;onWeek.accept(direction); // Zero means smooth rebound, never an unintended switch.
            }
            return true;
        }
        return super.onTouchEvent(e);
    }
    @Override protected void onDetachedFromWindow(){release();super.onDetachedFromWindow();}
}
