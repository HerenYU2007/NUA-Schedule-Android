package cn.nua.wenguan.schedule.data;

import java.net.URI;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Pure policy shared by the Android browser and regression tests. */
public final class SchoolSyncPolicy {
    private SchoolSyncPolicy() { }
    private static boolean schoolHost(String host) {
        return host != null && (host.equalsIgnoreCase("nua.edu.cn")
                || host.toLowerCase(Locale.ROOT).endsWith(".nua.edu.cn"));
    }
    public static boolean trusted(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getUserInfo() == null && schoolHost(host);
        } catch (RuntimeException ignored) { return false; }
    }
    public static boolean canRead(String url) {
        if (!trusted(url)) return false;
        URI uri = URI.create(url);
        return !"c.nua.edu.cn".equalsIgnoreCase(uri.getHost())
                && !uri.getPath().toLowerCase(Locale.ROOT).matches(".*/cas/(login|logout)(/.*)?");
    }
    public static boolean isBlankWindow(String url) {
        if (url == null) return false;
        String base = url.split("#", 2)[0];
        return "about:blank".equalsIgnoreCase(base);
    }
    /** Only an observed, query-free portal can be revisited; never replay an SSO ticket. */
    public static String portalRetryFor(String url) {
        if (!canRead(url)) return "";
        URI u=URI.create(url);String host=u.getHost(), path=u.getRawPath();
        if (u.getPort()!=-1&&u.getPort()!=443) return "";
        if ("i.nua.edu.cn".equalsIgnoreCase(host) && ("/".equals(path)||"/sopcb/".equals(path)||"/sopcb".equals(path))) return "https://i.nua.edu.cn"+path;
        if ("v.nua.edu.cn".equalsIgnoreCase(host) && path!=null && path.matches("/https/[A-Za-z0-9_=-]{24,512}/(?:sopcb/?)?")) return "https://v.nua.edu.cn"+path;
        return "";
    }
    /** Recognizes an address, not an authenticated session: this URL can return a login form. */
    public static boolean isAcademicPage(String url) {
        if (!canRead(url)) return false;
        URI uri = URI.create(url);
        return "jwnew.nua.edu.cn".equalsIgnoreCase(uri.getHost()) && uri.getPath().startsWith("/jsxsd/");
    }
    public static boolean isAcademicHome(String url) {
        if (!trusted(url)) return false;
        URI uri = URI.create(url);
        return "jwnew.nua.edu.cn".equalsIgnoreCase(uri.getHost())
                && "/jsxsd/framework/xsMain.jsp".equals(uri.getPath());
    }
    public static SyncAttemptPolicy.Route syncRoute(String url) {
        if (!trusted(url)) return SyncAttemptPolicy.Route.AUTO;
        String host = URI.create(url).getHost();
        if ("v.nua.edu.cn".equalsIgnoreCase(host)) return SyncAttemptPolicy.Route.VPN;
        return "jwnew.nua.edu.cn".equalsIgnoreCase(host)
                ? SyncAttemptPolicy.Route.DIRECT : SyncAttemptPolicy.Route.AUTO;
    }
    public static boolean isAcademicTransportPage(String url) {
        return isAcademicPage(url) || !vpnAcademicPrefix(url).isEmpty();
    }
    public static String academicHomeFor(String url) {
        if (isAcademicPage(url)) return "https://jwnew.nua.edu.cn/jsxsd/framework/xsMain.jsp";
        String prefix = vpnAcademicPrefix(url);
        return prefix.isEmpty() ? "" : "https://v.nua.edu.cn" + prefix + "/jsxsd/framework/xsMain.jsp";
    }
    public static boolean isPersonalSettings(String url) {
        if (!trusted(url)) return false;
        URI uri = URI.create(url);
        String prefix = vpnAcademicPrefix(url);
        String path = prefix.isEmpty() ? uri.getPath() : uri.getPath().substring(prefix.length());
        return ("jwnew.nua.edu.cn".equalsIgnoreCase(uri.getHost()) || !prefix.isEmpty())
                && path.toLowerCase(Locale.ROOT).startsWith("/jsxsd/grsz/");
    }
    public static boolean canAutoOpenFrame(String currentUrl, String frameUrl) {
        if (!canRead(currentUrl) || !isAcademicHome(frameUrl)) return false;
        URI current = URI.create(currentUrl), frame = URI.create(frameUrl);
        if ("jwnew.nua.edu.cn".equalsIgnoreCase(current.getHost())) return false;
        return (frame.getPort() == -1 || frame.getPort() == 443)
                && (frame.getQuery() == null || !frame.getQuery().matches("(?i).*(?:^|&)(ticket|token|code|password)=.*"));
    }
    public static boolean isAcademicDownload(String url) {
        if (!trusted(url)) return false;
        URI uri = URI.create(url);
        return "jwnew.nua.edu.cn".equalsIgnoreCase(uri.getHost())
                && (uri.getPort() == -1 || uri.getPort() == 443)
                && !uri.getPath().matches("(?i).*/(?:cas|sso\\.jsp)(?:/.*)?")
                && (uri.getQuery() == null || !uri.getQuery().matches("(?i).*(?:^|[&])(ticket|password)=.*"));
    }
    /** WebVPN routes are opaque: bind to the currently opened academic page, never guess a token. */
    private static String vpnAcademicPrefix(String url) {
        if (!canRead(url)) return "";
        URI uri = URI.create(url);
        if (!"v.nua.edu.cn".equalsIgnoreCase(uri.getHost())
                || (uri.getPort() != -1 && uri.getPort() != 443)) return "";
        String path = uri.getRawPath();
        // Do not interpret escaped separators, dot segments, wildcard redactions or arbitrary proxies.
        if (path == null || !path.equals(uri.getPath()) || path.contains("\\")
                || path.contains("//") || path.matches(".*/\\.{1,2}(?:/.*)?")) return "";
        java.util.regex.Matcher route = java.util.regex.Pattern.compile(
                "^(/https/[A-Za-z0-9_=-]{24,512})/jsxsd/[A-Za-z0-9_./-]*$").matcher(path);
        return route.matches() ? route.group(1) : "";
    }
    public static boolean isAcademicDownload(String url, String sourcePage) {
        return !academicDownloadReferer(url, sourcePage).isEmpty();
    }
    /** Also provides the query-free route identity used to constrain every redirect hop. */
    public static String academicDownloadReferer(String url, String sourcePage) {
        if (isAcademicDownload(url)) return "https://jwnew.nua.edu.cn/jsxsd/framework/xsMain.jsp";
        String prefix = vpnAcademicPrefix(url);
        if (prefix.isEmpty() || !prefix.equals(vpnAcademicPrefix(sourcePage))) return "";
        URI download = URI.create(url), source = URI.create(sourcePage);
        String relative = download.getPath().substring(prefix.length());
        String sourceRelative = source.getPath().substring(prefix.length());
        if (!relative.startsWith("/jsxsd/xskb/") || sourceRelative.startsWith("/jsxsd/grsz/")
                || (download.getQuery() != null
                    && download.getQuery().matches("(?i).*(?:^|&)(ticket|password)=.*"))) return "";
        return "https://v.nua.edu.cn" + prefix + "/jsxsd/framework/xsMain.jsp";
    }
    public static String secureHttpFallback(String url, String method) {
        if (!"GET".equalsIgnoreCase(method)) return ""; // Never reconstruct or replay a POST body.
        try {
            URI uri = URI.create(url);
            if (!"http".equalsIgnoreCase(uri.getScheme()) || !schoolHost(uri.getHost())
                    || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 80)) return "";
            String path = uri.getRawPath() == null ? "" : uri.getRawPath();
            String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
            String fragment = uri.getRawFragment() == null ? "" : "#" + uri.getRawFragment();
            // Preserve encoded query bytes (including SSO parameters), but send only over HTTPS.
            return "https://" + uri.getHost() + path + query + fragment;
        } catch (RuntimeException ignored) { return ""; }
    }
    public enum UpgradeResult { ALLOW, DUPLICATE, LIMIT, INVALID }

    /** Per-window, memory-only guard. Diagnostic redaction must never determine request identity. */
    public static final class SecureUpgradeGuard {
        public static final int MAX_ATTEMPTS = 8;
        public static final int MAX_DUPLICATES = 8;
        private final Set<String> fingerprints = new HashSet<>();
        private int duplicateCount;
        public int getAttemptCount() { return fingerprints.size(); }
        public int getDuplicateCount() { return duplicateCount; }
        public UpgradeResult register(String secureUrl) {
            if (!trusted(secureUrl)) return UpgradeResult.INVALID;
            try {
                URI uri = URI.create(secureUrl);
                // CAS entry and CAS ticket callback share a path but have different query bytes.
                // Keep only a digest in memory, never raw tickets, and ignore non-request fragments.
                String identity = uri.getHost().toLowerCase(Locale.ROOT)
                        + (uri.getPort() == -1 || uri.getPort() == 443 ? "" : ":" + uri.getPort())
                        + (uri.getRawPath() == null ? "" : uri.getRawPath())
                        + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
                String key = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256")
                        .digest(identity.getBytes(StandardCharsets.UTF_8)));
                if (fingerprints.contains(key)) {
                    return ++duplicateCount <= MAX_DUPLICATES ? UpgradeResult.DUPLICATE : UpgradeResult.LIMIT;
                }
                if (fingerprints.size() >= MAX_ATTEMPTS) return UpgradeResult.LIMIT;
                fingerprints.add(key);
                return UpgradeResult.ALLOW;
            } catch (RuntimeException | NoSuchAlgorithmException ignored) { return UpgradeResult.INVALID; }
        }
    }
    public static String displayAddress(String url) {
        if (url == null || url.isEmpty()) return "尚无页面地址";
        if (isBlankWindow(url)) return "about:blank（新窗口占位页）";
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme() == null ? "无协议" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (uri.getHost() == null) return scheme + ":（省略地址正文）";
            // Never retain userinfo, query parameters, fragments, Cookie, tickets, or form data.
            String path = uri.getPath() == null ? "" : uri.getPath();
            path = path.replaceAll("(?i)ST-[^/]+", "*").replaceAll("[A-Za-z0-9_=-]{24,}", "*")
                    .replaceAll("[0-9]{6,}", "*");
            if (path.length() > 100) path = path.substring(0, 100) + "…";
            String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
            return scheme + "://" + uri.getHost() + port + path;
        } catch (RuntimeException ignored) { return "地址格式无法识别（省略正文）"; }
    }
}
