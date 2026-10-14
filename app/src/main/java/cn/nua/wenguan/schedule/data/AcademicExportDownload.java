package cn.nua.wenguan.schedule.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import javax.net.ssl.HttpsURLConnection;

/** Memory-only, HTTPS-only retrieval of an actual WebView export URL; no invented endpoints. */
public final class AcademicExportDownload {
    public interface CookieSource { String forUrl(String url); }
    public interface Progress { void status(String summary, String url); }
    private AcademicExportDownload() { }

    public static byte[] fetch(String actualUrl, String userAgent, CookieSource cookies) throws IOException {
        return fetch(actualUrl, userAgent, cookies, null);
    }

    public static byte[] fetch(String actualUrl, String userAgent, CookieSource cookies,
                               String sourcePage) throws IOException {
        return fetch(actualUrl, userAgent, cookies, sourcePage, null);
    }

    public static byte[] fetch(String actualUrl, String userAgent, CookieSource cookies,
                               String sourcePage, Progress progress) throws IOException {
        String referer = SchoolSyncPolicy.academicDownloadReferer(actualUrl, sourcePage);
        if (referer.isEmpty()) throw new IOException("导出不是当前教务页面的 HTTPS 地址");
        String url = actualUrl;
        for (int hop = 0; hop < 6; hop++) {
            if (Thread.currentThread().isInterrupted()) throw new IOException("下载已取消");
            if (!referer.equals(SchoolSyncPolicy.academicDownloadReferer(url, sourcePage)))
                throw new IOException("导出跳转离开了当前教务 HTTPS 路径");
            HttpsURLConnection connection = (HttpsURLConnection) URI.create(url).toURL().openConnection();
            try {
                // Default platform certificate and hostname checks remain untouched.
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setUseCaches(false);
                connection.setRequestProperty("User-Agent", userAgent);
                connection.setRequestProperty("Referer", referer);
                String cookie = cookies.forUrl(url);
                if (cookie != null && !cookie.isEmpty()) connection.setRequestProperty("Cookie", cookie);
                int status = connection.getResponseCode();
                if (progress != null) {
                    String mime = connection.getContentType();
                    mime = mime == null ? "" : mime.toLowerCase(java.util.Locale.ROOT);
                    String type = mime.contains("html") ? "HTML页面" : mime.contains("excel")
                            ? "Excel文件" : mime.contains("octet-stream") ? "二进制文件" : "其他或未标明";
                    progress.status("XLS下载第" + (hop + 1) + "跳：HTTP " + status + "，类型=" + type, url);
                }
                if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("导出跳转缺少地址");
                    try { url = URI.create(url).resolve(location).toString(); }
                    catch (IllegalArgumentException error) { throw new IOException("导出跳转格式错误"); }
                    continue;
                }
                if (status != 200) throw new IOException("学校导出未返回文件");
                long length = connection.getContentLengthLong();
                if (length > ExportedXlsReader.MAX_BYTES) throw new IOException("导出超过 5 MB");
                try (InputStream input = connection.getInputStream()) { return ExportedXlsReader.readBounded(input); }
            } finally { connection.disconnect(); }
        }
        throw new IOException("导出跳转次数过多");
    }
}
