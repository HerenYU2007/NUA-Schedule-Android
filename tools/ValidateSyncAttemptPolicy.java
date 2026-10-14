import cn.nua.wenguan.schedule.data.SyncAttemptPolicy;
import cn.nua.wenguan.schedule.data.SchoolSyncPolicy;

public final class ValidateSyncAttemptPolicy {
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        for (SyncAttemptPolicy.Route route : new SyncAttemptPolicy.Route[]{
                SyncAttemptPolicy.Route.DIRECT, SyncAttemptPolicy.Route.VPN}) {
            SyncAttemptPolicy policy = new SyncAttemptPolicy();
            policy.reset(); policy.observe(route);
            SyncAttemptPolicy.Route opposite = route == SyncAttemptPolicy.Route.VPN
                    ? SyncAttemptPolicy.Route.DIRECT : SyncAttemptPolicy.Route.VPN;
            require(policy.fail().name().equals(opposite.name()), "First failure selects opposite observed route");
            require(policy.failures() == 1 && !policy.isRunning(), "Switching freezes old attempt");
            require(policy.fail() == SyncAttemptPolicy.Next.IGNORE, "Duplicate old failures ignored");
            require(policy.startFallback(), "One fallback starts");
            require(!policy.startFallback(), "Fallback cannot start twice");
            require(policy.accepts(opposite) && !policy.accepts(route), "Fallback must use opposite academic route");
            policy.observe(route);
            require(policy.expected() == opposite, "School redirect cannot silently change selected fallback");
            require(policy.fail() == SyncAttemptPolicy.Next.STOP, "Second failure stops immediately");
            require(policy.failures() == 2 && policy.fail() == SyncAttemptPolicy.Next.IGNORE, "No third failure or route");
            require(!policy.startFallback(), "No fallback after terminal error");
            policy.reset(); policy.observe(route); policy.succeed();
            require(!policy.isRunning() && policy.fail() == SyncAttemptPolicy.Next.IGNORE, "First success skips other route");
            policy.reset(); policy.observe(route); policy.fail(); policy.startFallback(); policy.succeed();
            require(policy.fail() == SyncAttemptPolicy.Next.IGNORE, "Fallback success ignores late errors");
        }
        SyncAttemptPolicy unknown = new SyncAttemptPolicy(); unknown.reset();
        require(unknown.fail() == SyncAttemptPolicy.Next.VPN, "Unknown portal error uses known school VPN entry");
        unknown.reset(); require(unknown.failures() == 0 && unknown.expected() == SyncAttemptPolicy.Route.AUTO,
                "Manual restart gets fresh two-attempt budget");
        String vpn = "https://v.nua.edu.cn/https/0123456789abcdef0123456789abcdefabcdef0123456789/jsxsd/";
        require(SchoolSyncPolicy.syncRoute(vpn) == SyncAttemptPolicy.Route.VPN, "School gateway route detected");
        require(SchoolSyncPolicy.syncRoute("https://jwnew.nua.edu.cn/sso.jsp") == SyncAttemptPolicy.Route.DIRECT, "SSO direct detected");
        require(SchoolSyncPolicy.syncRoute("https://c.nua.edu.cn/cas/login") == SyncAttemptPolicy.Route.AUTO, "CAS does not guess campus route");
        require(SchoolSyncPolicy.syncRoute("https://v.nua.edu.cn.attacker.example/") == SyncAttemptPolicy.Route.AUTO, "Spoofed route ignored");
        require(SchoolSyncPolicy.isAcademicTransportPage(vpn + "framework/xsMain.jsp"), "VPN academic transport recognized");
        require(SchoolSyncPolicy.isPersonalSettings(vpn + "grsz/grsz_xgrxx.do"), "VPN settings protected natively");
        require(SchoolSyncPolicy.academicHomeFor(vpn + "grsz/grsz_xgrxx.do").equals(vpn + "framework/xsMain.jsp"), "VPN profile recovery stays in same route");
        require(!SchoolSyncPolicy.isAcademicTransportPage("https://v.nua.edu.cn/"), "VPN entry not academic success");
        System.out.println("Two-route sync policy passed: either order, duplicate/stale failures, strict fallback route, two-error stop, early success and restart.");
    }
}
