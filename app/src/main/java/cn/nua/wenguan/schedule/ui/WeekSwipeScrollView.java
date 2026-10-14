package cn.nua.wenguan.schedule.ui;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ScrollView;
import java.util.function.IntConsumer;

/** Horizontal gestures cancel child clicks; vertical scrolling and ordinary taps are unchanged. */
public final class WeekSwipeScrollView extends ScrollView {
    private float x, y; private boolean horizontal, vertical, cancelled;
    private IntConsumer onWeek;
    public WeekSwipeScrollView(Context c, AttributeSet attrs) { super(c, attrs); }
    public void setOnWeekSwipe(IntConsumer callback) { onWeek = callback; }
    @Override public boolean onInterceptTouchEvent(MotionEvent e) {
        if (onWeek == null) return super.onInterceptTouchEvent(e);
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) { x=e.getX(); y=e.getY(); horizontal=vertical=cancelled=false; }
        if (e.getPointerCount() > 1) { cancelled = true; return super.onInterceptTouchEvent(e); }
        if (e.getActionMasked() == MotionEvent.ACTION_MOVE && !cancelled) {
            float dx=e.getX()-x, dy=e.getY()-y; int slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();
            if (Math.abs(dy)>slop && Math.abs(dy)>Math.abs(dx)) vertical=true;
            if (!vertical && WeekSwipePolicy.direction(dx,dy,slop*2f)!=0) horizontal=true;
            if (horizontal) return true;
        }
        return super.onInterceptTouchEvent(e);
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getPointerCount()>1) cancelled=true;
        if (horizontal && onWeek != null) {
            if (e.getActionMasked()==MotionEvent.ACTION_UP) {
                int direction=WeekSwipePolicy.direction(e.getX()-x,e.getY()-y,48*getResources().getDisplayMetrics().density);
                horizontal=false;
                if (!cancelled && direction!=0) onWeek.accept(direction);
            } else if(e.getActionMasked()==MotionEvent.ACTION_CANCEL) horizontal=false;
            return true;
        }
        return super.onTouchEvent(e);
    }
}
