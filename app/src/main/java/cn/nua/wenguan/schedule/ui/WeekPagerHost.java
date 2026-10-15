package cn.nua.wenguan.schedule.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.*;
import android.widget.FrameLayout;
import java.util.function.*;

/** Stationary parent owns horizontal gestures; moving pages keep native vertical scrolling. */
public final class WeekPagerHost extends FrameLayout {
    private boolean enabled,touching,horizontal,vertical,cancelled,captured;
    private boolean verticalTail;
    private float downX,downY;
    private VelocityTracker velocity;
    private Runnable begin=()->{};
    private DoubleConsumer drag=value->{};
    private BiConsumer<Integer,Boolean> release=(direction,cancelled)->{};
    private BooleanSupplier settling=()->false;
    private Runnable finished=()->{};
    public WeekPagerHost(Context c,AttributeSet attrs){super(c,attrs);}
    public void setCallbacks(Runnable begin,DoubleConsumer drag,BiConsumer<Integer,Boolean> release,BooleanSupplier settling){this.begin=begin;this.drag=drag;this.release=release;this.settling=settling;}
    public void setOnGestureFinished(Runnable callback){finished=callback;}
    public void setPagingEnabled(boolean value){if(enabled&&!value)cancelGesture();enabled=value;}
    public boolean isTouching(){return touching;}
    public boolean hasVerticalTail(){return verticalTail;}
    public void cancelGesture(){touching=horizontal=vertical=cancelled=captured=false;if(velocity!=null){velocity.recycle();velocity=null;}}
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        if(!enabled)return super.dispatchTouchEvent(event);
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){
            cancelGesture();verticalTail=false;touching=true;captured=settling.getAsBoolean();downX=event.getRawX();downY=event.getRawY();velocity=VelocityTracker.obtain();begin.run();
        }
        if(event.getPointerCount()>1)cancelled=true;
        // This host never translates, so no copied MotionEvent or per-MOVE coordinate conversion.
        if(velocity!=null)velocity.addMovement(event);
        try{return super.dispatchTouchEvent(event);}
        finally{if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){verticalTail=vertical&&!horizontal&&!captured;cancelGesture();finished.run();}}
    }
    private void decide(MotionEvent event){
        if(horizontal||vertical||cancelled)return;
        int intent=WeekSwipePolicy.lock(event.getRawX()-downX,event.getRawY()-downY,ViewConfiguration.get(getContext()).getScaledTouchSlop());horizontal=intent==1;vertical=intent==2;
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent event){
        if(!enabled)return false;
        if(captured)return true;
        if(event.getActionMasked()==MotionEvent.ACTION_MOVE)decide(event);
        return horizontal;
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(!enabled)return super.onTouchEvent(event);
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_MOVE){decide(event);if((horizontal||captured)&&!cancelled)drag.accept(event.getRawX()-downX);}
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
            if(horizontal||captured){
                float speed=0;if(velocity!=null){velocity.computeCurrentVelocity(1000,ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity());speed=velocity.getXVelocity();}
                boolean aborted=cancelled||action==MotionEvent.ACTION_CANCEL;
                int direction=aborted?0:WeekSwipePolicy.directionWithVelocity(event.getRawX()-downX,event.getRawY()-downY,speed,getResources().getDisplayMetrics().density);
                release.accept(direction,aborted);
            }else if(action==MotionEvent.ACTION_UP&&!cancelled)performClick();
        }
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    @Override protected void onDetachedFromWindow(){cancelGesture();super.onDetachedFromWindow();}
}
