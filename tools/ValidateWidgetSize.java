import cn.nua.wenguan.schedule.widget.WidgetSizePolicy;
import java.util.Arrays;
public final class ValidateWidgetSize {
    private static void require(boolean value, String msg) { if (!value) throw new AssertionError(msg); }
    public static void main(String[] args) {
        require(Arrays.equals(WidgetSizePolicy.range(0,0,250), new int[]{250,250}), "zero host dimensions use a coherent default");
        require(Arrays.equals(WidgetSizePolicy.range(450,240,250), new int[]{240,450}), "inverted range normalized");
        require(WidgetSizePolicy.current(320,220,320,700,120,220), "portrait exact size retained");
        require(WidgetSizePolicy.current(700,120,320,700,120,220), "landscape exact size retained");
        require(!WidgetSizePolicy.current(250,120,320,320,200,220), "stale pre-resize size rejected");
        require(WidgetSizePolicy.current(250,120,0,0,0,0), "exact-only host accepted");
        require(!WidgetSizePolicy.current(Float.NaN,120,0,0,0,0), "invalid exact size rejected");
        for (int value : new int[]{-10,0,1,100,250,700}) {
            int[] range = WidgetSizePolicy.range(value, value - 10, 250);
            require(range[0] > 0 && range[1] >= range[0], "fallback range always valid");
        }
        System.out.println("Widget resize tests passed: missing/zero/inverted/stale options, exact sizes, both orientations.");
    }
}
