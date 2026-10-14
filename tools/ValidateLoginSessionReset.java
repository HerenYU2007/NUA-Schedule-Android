import cn.nua.wenguan.schedule.data.LoginSessionReset;

public final class ValidateLoginSessionReset {
    public static void main(String[] args) {
        LoginSessionReset gate = new LoginSessionReset();
        require(!gate.takeReady(true), "未清空不能登录");
        int first = gate.begin();
        require(gate.isClearing() && !gate.takeReady(true), "等待回调期间不能登录");
        require(gate.complete(first, true), "空Cookie回调完成");
        require(!gate.takeReady(false), "后台不加载入口");
        require(gate.takeReady(true) && !gate.takeReady(true), "恢复前台只打开一次");
        require(!gate.complete(first, true), "重复回调无效");
        int failed = gate.begin();
        require(gate.complete(failed, false) && !gate.takeReady(true), "Cookie未清空不能登录");
        int stale = gate.begin();
        int current = gate.begin();
        require(!gate.complete(stale, true) && gate.isClearing(), "旧回调不结束新重试");
        require(gate.complete(current, true) && gate.takeReady(true), "新请求正常完成");
        int closing = gate.begin();
        gate.invalidate();
        require(!gate.complete(closing, true) && !gate.takeReady(true), "关闭页面后不恢复旧登录");
        System.out.println("Login reset validation passed: async callback, empty/failure, foreground resume, duplicate/stale and destruction guards.");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
