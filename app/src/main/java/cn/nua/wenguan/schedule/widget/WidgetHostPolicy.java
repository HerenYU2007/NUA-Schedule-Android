package cn.nua.wenguan.schedule.widget;

/** Phone launchers get one complete layout; tablet responsive variants remain unchanged. */
public final class WidgetHostPolicy {
    private WidgetHostPolicy() { }
    public static boolean singleLayout(int smallestWidthDp){return smallestWidthDp<600;}
    public static int[] currentSize(int minWidth,int maxWidth,int minHeight,int maxHeight,int fallbackWidth,int fallbackHeight,boolean landscape){
        int[] w=WidgetSizePolicy.range(minWidth,maxWidth,fallbackWidth),h=WidgetSizePolicy.range(minHeight,maxHeight,fallbackHeight);
        return landscape?new int[]{w[1],h[0]}:new int[]{w[0],h[1]};
    }
}
