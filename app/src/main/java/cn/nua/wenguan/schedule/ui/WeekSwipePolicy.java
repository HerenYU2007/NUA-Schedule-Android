package cn.nua.wenguan.schedule.ui;
public final class WeekSwipePolicy {
    private WeekSwipePolicy() { }
    public static int direction(float dx, float dy, float threshold) {
        if (!Float.isFinite(dx) || !Float.isFinite(dy) || Math.abs(dx) < threshold || Math.abs(dx) < Math.abs(dy) * .85f) return 0;
        return dx < 0 ? 1 : -1;
    }
    public static int directionWithVelocity(float dx,float dy,float velocity,float density){
        if(!Float.isFinite(density)||density<=0||!Float.isFinite(velocity))return 0;
        float threshold=Math.abs(velocity)>600*density&&Math.signum(velocity)==Math.signum(dx)?24*density:48*density;
        return direction(dx,dy,threshold);
    }
    /** 0 undecided, 1 horizontal, 2 vertical. Ignore early diagonal jitter near touch slop. */
    public static int lock(float dx,float dy,float slop){
        if(!Float.isFinite(dx)||!Float.isFinite(dy)||slop<=0)return 0;
        float x=Math.abs(dx),y=Math.abs(dy);
        if(y>slop*1.5f&&y>x*1.4f)return 2;
        if(x>slop*1.5f&&x>=y*.85f)return 1;
        if(Math.max(x,y)>slop*2.5f)return x>=y*.85f?1:2;
        return 0;
    }
}
