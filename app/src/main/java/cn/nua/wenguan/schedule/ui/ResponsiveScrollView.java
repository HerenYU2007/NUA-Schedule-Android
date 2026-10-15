package cn.nua.wenguan.schedule.ui;
import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

/** Native drag/fling physics; no per-MOVE scrollTo/smoothScrollTo or velocity multiplier. */
public class ResponsiveScrollView extends ScrollView {
    private boolean touching;
    private long lastMotion;
    public ResponsiveScrollView(Context c){super(c);configure();}
    public ResponsiveScrollView(Context c,AttributeSet attrs){super(c,attrs);configure();}
    private void configure(){
        setSmoothScrollingEnabled(true);
        setVerticalFadingEdgeEnabled(false);
        setHorizontalFadingEdgeEnabled(false);
    }
    @Override public boolean dispatchTouchEvent(MotionEvent e){
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN)touching=true;
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)touching=false;
        lastMotion=SystemClock.uptimeMillis();
        return super.dispatchTouchEvent(e);
    }
    @Override protected void onScrollChanged(int x,int y,int oldX,int oldY){
        lastMotion=SystemClock.uptimeMillis();super.onScrollChanged(x,y,oldX,oldY);
    }
    public boolean isInteracting(){return touching||SystemClock.uptimeMillis()-lastMotion<180;}
    public void stopInertia(){fling(0);}
    @Override protected void onDetachedFromWindow(){touching=false;super.onDetachedFromWindow();}
}
