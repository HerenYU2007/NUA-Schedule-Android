package cn.nua.wenguan.schedule.ui;

/** A drag/multi-touch that returns to its origin must still not count as an outside tap. */
public final class OutsideTapPolicy {
    private double x,y,slop;
    private boolean candidate;
    public void down(double x,double y,double slop,boolean outside){this.x=x;this.y=y;this.slop=Math.max(0,slop);candidate=outside;}
    public void move(double x,double y){if(Math.hypot(x-this.x,y-this.y)>slop)candidate=false;}
    public void cancel(){candidate=false;}
    public boolean up(double x,double y){move(x,y);boolean result=candidate;candidate=false;return result;}
}
