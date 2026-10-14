package cn.nua.wenguan.schedule.data;

/** One automatic/observed route, one opposite-route fallback, no per-error retry loops. */
public final class SyncAttemptPolicy {
    public enum Route { AUTO, DIRECT, VPN }
    public enum Next { IGNORE, DIRECT, VPN, STOP }
    private Route expected = Route.AUTO, observed = Route.AUTO;
    private int failures;
    private boolean running, switching, terminal;
    public void reset() {
        expected = observed = Route.AUTO;
        failures = 0;
        running = true;
        switching = terminal = false;
    }
    public void observe(Route route) {
        if (running && !terminal && route != Route.AUTO) observed = route;
    }
    public boolean accepts(Route route) {
        return expected == Route.AUTO || route == Route.AUTO || expected == route;
    }
    public Next fail() {
        if (!running || terminal || switching) return Next.IGNORE;
        failures++;
        running = false;
        if (failures >= 2) { terminal = true; return Next.STOP; }
        Route failed = expected == Route.AUTO ? observed : expected;
        expected = failed == Route.VPN ? Route.DIRECT : Route.VPN;
        switching = true;
        return expected == Route.VPN ? Next.VPN : Next.DIRECT;
    }
    public boolean startFallback() {
        if (!switching || terminal) return false;
        switching = false;
        running = true;
        observed = Route.AUTO;
        return true;
    }
    public void succeed() { running = switching = false; terminal = true; }
    public boolean isRunning() { return running && !terminal; }
    public int failures() { return failures; }
    public Route expected() { return expected; }
}
