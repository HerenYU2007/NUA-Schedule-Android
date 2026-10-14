package cn.nua.wenguan.schedule.data;

/** Callback gate: a fresh login may begin only after cookie removal completes, in the foreground. */
public final class LoginSessionReset {
    private int generation;
    private boolean clearing;
    private boolean ready;

    public int begin() {
        clearing = true;
        ready = false;
        return ++generation;
    }
    public boolean complete(int token, boolean cookiesEmpty) {
        if (token != generation || !clearing) return false;
        clearing = false;
        ready = cookiesEmpty;
        return true;
    }
    public boolean takeReady(boolean foreground) {
        if (!foreground || !ready || clearing) return false;
        ready = false;
        return true;
    }
    public boolean isClearing() { return clearing; }
    public void invalidate() { generation++; clearing = false; ready = false; }
}
