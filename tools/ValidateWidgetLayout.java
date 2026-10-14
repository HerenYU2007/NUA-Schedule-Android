import cn.nua.wenguan.schedule.widget.WidgetLayoutPolicy;

public final class ValidateWidgetLayout {
    public static void main(String[] args) {
        require(new WidgetLayoutPolicy(180, 100, 1).rows == 1, "较小桌面格子应显示一节完整课程");
        require(new WidgetLayoutPolicy(250, 120, 1.3f).rows == 1, "默认大小、大字号至少显示一节完整课程");
        require(new WidgetLayoutPolicy(700, 120, 1).rows == 1, "横屏短组件不强行显示三节");
        require(new WidgetLayoutPolicy(480, 360, 1).rows == 3, "平板拉高后显示三节课程");
        require(!new WidgetLayoutPolicy(480, 360, 1).compact, "平板大组件使用大字号");
        WidgetLayoutPolicy phone = new WidgetLayoutPolicy(320, 220, 1, false);
        require(phone.phoneLayout && phone.padding == 14 && phone.headerSize == 15 && phone.timeSize == 13, "手机恢复原留白和字号");
        require(phone.rowsForCount(3) == 3, "正常手机4×2高度可显示三节紧凑课程");
        require(phone.rowsForCount(4) == 2, "更多课程时为溢出提示预留空间");
        require(new WidgetLayoutPolicy(700, 120, 1, false).phoneLayout, "手机横屏不误用平板布局");
        require(new WidgetLayoutPolicy(320, 160, 1, false).rowsForCount(3) == 1, "矮手机组件不强塞课程");
        require(!new WidgetLayoutPolicy(180, 100, 1, false).phoneLayout, "极窄手机回退分行布局保留时间和地点");
        for (float width : new float[] {160, 180, 250, 320, 480, 700, Float.NaN}) {
            for (float height : new float[] {60, 100, 120, 180, 220, 360, Float.NaN}) {
                for (float scale : new float[] {0.8f, 1, 1.3f, 2, Float.NaN}) {
                    WidgetLayoutPolicy layout = new WidgetLayoutPolicy(width, height, scale);
                    require(layout.rows >= 0 && layout.rows <= 3, "容量必须有效");
                    require(Float.isFinite(layout.nameSize), "无效桌面选项不得崩溃");
                    WidgetLayoutPolicy tablet = new WidgetLayoutPolicy(width, height, scale, true);
                    require(!tablet.phoneLayout && tablet.rows == layout.rows && tablet.padding == layout.padding
                            && tablet.timeSize == layout.timeSize && tablet.headerSize == layout.headerSize,
                            "平板原规则不改变");
                    verifyTabletBaseline(width, height, scale, tablet);
                }
            }
        }
        System.out.println("Widget layout validation passed: tablet portrait/landscape, resize, small host cells, font sizes and invalid/missing dimensions.");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void verifyTabletBaseline(float width, float height, float fontScale, WidgetLayoutPolicy actual) {
        // Independent 1.1.10 baseline: phone fixes must not change the working tablet sizing.
        if (!Float.isFinite(width) || width <= 0) width = 250;
        if (!Float.isFinite(height) || height <= 0) height = 120;
        float scale = Float.isFinite(fontScale) ? Math.max(1f, fontScale) : 1f;
        boolean compact = width < 360 || height < 220;
        int padding = compact ? 4 : 12;
        float effectiveScale = Math.min(scale, compact ? 1.15f : 1.3f);
        int rows = Math.max(0, Math.min(3, (int) ((height - padding * 2 - (compact ? 34 : 48) * effectiveScale)
                / ((compact ? 56 : 72) * effectiveScale))));
        require(actual.compact == compact && actual.padding == padding && actual.rows == rows
                && actual.timeSize == (compact ? 12 : 16) * effectiveScale
                && actual.nameSize == (compact ? 12 : 16) * effectiveScale
                && actual.detailSize == (compact ? 10 : 13) * effectiveScale
                && actual.statusSize == (compact ? 10 : 12) * effectiveScale
                && actual.headerSize == (compact ? 12 : 16) * effectiveScale
                && actual.footerSize == (compact ? 10 : 12) * effectiveScale, "平板尺寸/字号与1.1.10完全一致");
        for (int count = 0; count < 8; count++) require(actual.rowsForCount(count) == rows, "平板显示容量不改变");
    }
}
