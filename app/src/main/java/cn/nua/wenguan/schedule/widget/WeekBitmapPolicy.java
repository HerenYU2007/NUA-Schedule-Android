package cn.nua.wenguan.schedule.widget;
/** Plan real-pixel, 32-bit widget images while budgeting all responsive variants together. */
public final class WeekBitmapPolicy {
    private WeekBitmapPolicy() { }
    public static long totalBudget(int screenWidth,int screenHeight){
        long screen=(long)Math.max(1,screenWidth)*Math.max(1,screenHeight)*4;
        // Android allows 1.5 screenfuls. Use at most one, leaving headroom for host extras.
        return Math.min(24L*1024*1024,Math.max(64L*1024,screen));
    }
    public static int[] pixels(int widthDp,int heightDp,float density,long budget){
        int width=Math.max(1,widthDp),height=Math.max(1,heightDp);
        double desired=Float.isFinite(density)&&density>0?Math.min(4,density):1;
        double scale=Math.min(desired,Math.min(2048d/width,3072d/height));
        scale=Math.min(scale,Math.sqrt(Math.max(4,budget)/(4d*width*height)));
        return new int[]{Math.max(1,(int)(width*scale)),Math.max(1,(int)(height*scale))};
    }
}
