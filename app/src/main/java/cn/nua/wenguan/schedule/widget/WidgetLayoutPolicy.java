package cn.nua.wenguan.schedule.widget;

/** Pure sizing rules for each host size, in dp and effective sp. */
public final class WidgetLayoutPolicy {
    public final boolean compact;
    public final boolean phoneLayout;
    public final int rows;
    public final int padding;
    public final float timeSize, nameSize, detailSize, statusSize, headerSize, footerSize;
    private final float availableHeight, phoneRowHeight, phoneFooterHeight;

    public WidgetLayoutPolicy(float width, float height, float fontScale) {
        this(width, height, fontScale, true);
    }

    public WidgetLayoutPolicy(float width, float height, float fontScale, boolean tablet) {
        if (!Float.isFinite(width) || width <= 0) width = 250;
        if (!Float.isFinite(height) || height <= 0) height = 120;
        float scale = Float.isFinite(fontScale) ? Math.max(1f, fontScale) : 1f;
        phoneLayout = !tablet && width >= 240;
        if (phoneLayout) {
            compact = false;
            padding = height < 180 ? 8 : 14;
            float effectiveScale = Math.min(scale, 1.15f);
            availableHeight = height - padding * 2 - 40 * effectiveScale;
            phoneRowHeight = 48 * effectiveScale;
            phoneFooterHeight = 16 * effectiveScale;
            rows = Math.max(0, Math.min(3, (int) (availableHeight / phoneRowHeight)));
            timeSize = 13 * effectiveScale;
            nameSize = 12 * effectiveScale;
            detailSize = 10 * effectiveScale;
            statusSize = 11 * effectiveScale;
            headerSize = 15 * effectiveScale;
            footerSize = 11 * effectiveScale;
            return;
        }
        availableHeight = phoneRowHeight = phoneFooterHeight = 0;
        compact = width < 360 || height < 220;
        padding = compact ? 4 : 12;
        // Honour larger fonts up to 1.3x; budget space and apply explicit px to avoid host clipping.
        float effectiveScale = Math.min(scale, compact ? 1.15f : 1.3f);
        float rowHeight = (compact ? 56 : 72) * effectiveScale;
        float headerAndFooter = (compact ? 34 : 48) * effectiveScale;
        rows = Math.max(0, Math.min(3, (int) ((height - padding * 2 - headerAndFooter) / rowHeight)));
        timeSize = (compact ? 12 : 16) * effectiveScale;
        nameSize = (compact ? 12 : 16) * effectiveScale;
        detailSize = (compact ? 10 : 13) * effectiveScale;
        statusSize = (compact ? 10 : 12) * effectiveScale;
        headerSize = (compact ? 12 : 16) * effectiveScale;
        footerSize = (compact ? 10 : 12) * effectiveScale;
    }

    public int rowsForCount(int courseCount) {
        if (!phoneLayout || courseCount <= rows) return rows;
        return Math.max(0, Math.min(rows, (int) ((availableHeight - phoneFooterHeight) / phoneRowHeight)));
    }
}
