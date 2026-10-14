import cn.nua.wenguan.schedule.data.AcademicExportDownload;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;

/** In-process HTTPS fixtures; no school connection, credentials or weakened TLS settings. */
public class ValidateAcademicExportDownload {
    private static final Map<String, Reply> replies = new HashMap<>();
    private static final List<MockConnection> opened = new ArrayList<>();
    private static final List<String> cookieReads = new ArrayList<>();
    private static final byte[] body = {1, 2, 3, 4};
    private static final String route = "/https/0123456789abcdef0123456789abcdefabcdef0123456789";
    private static final String home = "https://v.nua.edu.cn" + route + "/jsxsd/framework/xsMain.jsp";
    private static final String export = "https://v.nua.edu.cn" + route + "/jsxsd/xskb/xskb_print.do?term=1%2B2";
    private static class Reply {
        final int status;
        final String location;
        Reply(int status, String location) { this.status = status; this.location = location; }
    }
    private static class MockConnection extends HttpsURLConnection {
        final Reply reply;
        boolean disconnected;
        MockConnection(URL url, Reply reply) { super(url); this.reply = reply; }
        @Override public int getResponseCode() { return reply.status; }
        @Override public String getHeaderField(String name) { return "Location".equals(name) ? reply.location : null; }
        @Override public long getContentLengthLong() { return body.length; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(body); }
        @Override public void disconnect() { disconnected = true; }
        @Override public boolean usingProxy() { return false; }
        @Override public void connect() { }
        @Override public String getCipherSuite() { return "TEST-ONLY"; }
        @Override public Certificate[] getLocalCertificates() { return null; }
        @Override public Certificate[] getServerCertificates() { return new Certificate[0]; }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static String cookie(String url) { cookieReads.add(url); return "fixture=" + cookieReads.size(); }
    private static void reset() { replies.clear(); opened.clear(); cookieReads.clear(); }
    private static void rejected(String url, String context, int allowedHops) throws Exception {
        try {
            AcademicExportDownload.fetch(url, "Test-UA", ValidateAcademicExportDownload::cookie, context);
            throw new AssertionError("Rejected destination must fail");
        } catch (IOException expected) { }
        require(opened.size() == allowedHops && cookieReads.size() == allowedHops,
                "Rejected hop must not connect or read Cookie");
        for (MockConnection connection : opened) require(connection.disconnected, "Connection released on failure");
    }
    public static void main(String[] args) throws Exception {
        URL.setURLStreamHandlerFactory(protocol -> "https".equals(protocol) ? new URLStreamHandler() {
            @Override protected URLConnection openConnection(URL url) {
                Reply reply = replies.get(url.toString());
                if (reply == null) throw new AssertionError("Unexpected connection");
                MockConnection connection = new MockConnection(url, reply);
                opened.add(connection);
                return connection;
            }
        } : null);
        for (String url : new String[]{"http://jwnew.nua.edu.cn/export.xls", "file:///example.xls",
                "https://outside.example/export.xls", "https://jwnew.nua.edu.cn/sso.jsp?ticket=ST-test-only",
                export.replace("012345", "fedcba"), export.replace("/xskb/", "/grsz/")}) {
            reset(); rejected(url, home, 0);
        }
        reset(); rejected(export, null, 0);
        reset();
        String next = export.replace("xskb_print.do?term=1%2B2", "download.xls?term=1%2B2");
        replies.put(export, new Reply(302, "download.xls?term=1%2B2"));
        replies.put(next, new Reply(200, null));
        List<String> progress = new ArrayList<>();
        require(Arrays.equals(body, AcademicExportDownload.fetch(export, "Test-UA",
                ValidateAcademicExportDownload::cookie, home + "?private=test#private",
                (summary, url) -> progress.add(summary))), "Gateway bytes returned unchanged");
        require(progress.size() == 2 && progress.get(0).contains("HTTP 302")
                && progress.get(1).contains("HTTP 200"), "Bounded status progress for each hop");
        require(progress.stream().noneMatch(value -> value.contains("private") || value.contains("fixture=")
                || value.contains("term=") || value.contains(route)), "Status messages never contain credentials or raw routes");
        require(cookieReads.equals(Arrays.asList(export, next)), "Cookie queried using each actual gateway URL, not jwnew");
        for (int i = 0; i < opened.size(); i++) {
            MockConnection connection = opened.get(i);
            require(home.equals(connection.getRequestProperty("Referer")), "Same gateway Referer without private query");
            require(("fixture=" + (i + 1)).equals(connection.getRequestProperty("Cookie")), "Per-hop Cookie lookup");
            require("GET".equals(connection.getRequestMethod()) && !connection.getInstanceFollowRedirects(), "Manual GET redirects");
            require(!connection.getUseCaches() && connection.disconnected, "No cached export and connection released");
        }
        for (String target : new String[]{"http://v.nua.edu.cn" + route + "/jsxsd/xskb/file.xls",
                export.replace("012345", "fedcba"), "https://outside.example/file.xls",
                "https://jwnew.nua.edu.cn/jsxsd/xskb/file.xls", home,
                export + "&%74icket=ST-test-only"}) {
            reset(); replies.put(export, new Reply(302, target)); rejected(export, home, 1);
        }
        reset(); replies.put(export, new Reply(302, export)); rejected(export, home, 6);
        reset(); replies.put(export, new Reply(403, null)); rejected(export, home, 1);
        reset(); replies.put(export, new Reply(302, null)); rejected(export, home, 1);
        reset();
        String direct = "https://jwnew.nua.edu.cn/jsxsd/xskb/xskb_print.do?term=1%2B2";
        replies.put(direct, new Reply(200, null));
        require(Arrays.equals(body, AcademicExportDownload.fetch(direct, "Test-UA", ValidateAcademicExportDownload::cookie)),
                "Campus direct export remains compatible");
        require(cookieReads.equals(Arrays.asList(direct)), "Campus Cookie remains on direct URL");
        require("https://jwnew.nua.edu.cn/jsxsd/framework/xsMain.jsp".equals(opened.get(0).getRequestProperty("Referer")), "Campus Referer unchanged");
        reset(); replies.put(direct, new Reply(302, export)); rejected(direct, home, 1);
        reset(); Thread.currentThread().interrupt(); rejected(export, home, 0); Thread.interrupted();
        System.out.println("Export download fixtures passed: campus and WebVPN, scoped redirects, exact per-hop Cookies/Referer, cancellation and rejection before connection. No live network tested.");
    }
}
