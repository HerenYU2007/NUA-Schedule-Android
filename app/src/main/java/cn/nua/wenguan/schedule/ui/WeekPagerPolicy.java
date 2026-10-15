package cn.nua.wenguan.schedule.ui;

/** Three real pages share one translation; their edges always meet with no blank interval. */
public final class WeekPagerPolicy {
    private WeekPagerPolicy() { }
    public static float[] positions(float width,float drag){
        float w=Math.max(1,width),offset=Math.max(-w,Math.min(w,drag));
        return new float[]{offset-w,offset,offset+w};
    }
    public static int nearest(float offset,float width){return Math.abs(offset)>=Math.max(1,width)*.5f?(offset<0?1:-1):0;}
    public static float rebased(float offset,float width,int committed){return offset+committed*Math.max(1,width);}
    public static int release(float offset,float width,int gesture,boolean cancelled){
        if(cancelled)return 0;
        int side=offset<0?1:offset>0?-1:0;
        return gesture==0?nearest(offset,width):gesture==side?side:0;
    }
    public static long duration(float width,float from,float to){return Math.max(80,Math.min(220,Math.round(220*Math.abs(to-from)/Math.max(1,width))));}
}
