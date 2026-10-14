package cn.nua.wenguan.schedule.model;

public enum CourseStatus {
    UPCOMING("未开始"),
    IN_PROGRESS("正在进行"),
    ENDED("已结束");

    private final String label;

    CourseStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
