package cn.nua.wenguan.schedule;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebViewDatabase;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import cn.nua.wenguan.schedule.data.OfficialScheduleParser;
import cn.nua.wenguan.schedule.data.ScheduleRepository;
import cn.nua.wenguan.schedule.data.ScheduleRuleStore;
import cn.nua.wenguan.schedule.widget.TodayScheduleWidgetProvider;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import cn.nua.wenguan.schedule.data.SchoolSyncPolicy;
import cn.nua.wenguan.schedule.data.ExportedXlsReader;
import cn.nua.wenguan.schedule.data.AcademicExportDownload;
import cn.nua.wenguan.schedule.data.LoginSessionReset;
import cn.nua.wenguan.schedule.data.SyncAttemptPolicy;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import android.widget.FrameLayout;
import android.webkit.ConsoleMessage;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceResponse;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.SystemClock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ScheduleSyncActivity extends AppearanceActivity {
    private static final String PORTAL_URL = "https://i.nua.edu.cn/sopcb/";
    private static final String ACADEMIC_HOME = "https://jwnew.nua.edu.cn/jsxsd/framework/xsMain.jsp";
    private static final String VPN_ENTRY = "https://v.nua.edu.cn/";
    // This is the school's observed CAS portal entry, not a direct academic password login.
    private static final String DIRECT_ENTRY = "https://c.nua.edu.cn/cas/login?service=https%3A%2F%2Fi.nua.edu.cn%2Fsopcb";
    private static final long ATTEMPT_TIMEOUT_MS = 75000L;
    private final SyncAttemptPolicy attempts = new SyncAttemptPolicy();
    private AlertDialog failureDialog;
    private boolean failureNoticePending;
    private boolean localImport;
    private String confirmedPortal = "";
    private boolean academicVisited;
    private boolean sessionRecovered;
    private long pausedAt;
    private static final int PICK_XLS = 401;
    private final ExecutorService fileWorker = Executors.newSingleThreadExecutor();
    private final LoginSessionReset loginReset = new LoginSessionReset();
    private Future<?> fileTask;
    private int fileGeneration;
    private boolean readingFile;
    private long exportWaitUntil;
    private Runnable pendingFileResult;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<WebView> windows = new ArrayList<>();
    private final Set<WebView> pending = new HashSet<>();
    private final Map<WebView, WebView> openers = new HashMap<>();
    private final Map<WebView, SchoolSyncPolicy.SecureUpgradeGuard> secureFallbacks = new HashMap<>();
    private final Set<WebView> duplicateWindowNoted = new HashSet<>();
    private final List<String> diagnostics = new ArrayList<>();
    private FrameLayout browserContainer;
    private WebView webView;
    private ProgressBar progressBar;
    private TextView statusText;
    private TextView addressText;
    private TextView browserProblem;
    private Button importButton;
    private String navigationScript;
    private String extractionScript;
    private String lastPreviewJson = "";
    private int generation;
    private long flowDeadline;
    private boolean active;
    private boolean extracting;
    private boolean reviewing;
    private boolean recovering;
    private boolean flowStopped;
    private int blankCount;
    private int academicLoginCount;
    private int profileRecoveries;
    private boolean academicHomeNoted;
    private String lastPageStructure = "";
    private AlertDialog reviewDialog;
    private AlertDialog fetchingDialog;
    private boolean fetchingNoticeActive;
    private boolean fetchingNoticeHidden;
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (!active || reviewing || readingFile || flowStopped || webView == null) return;
            if (!canRead(webView.getUrl())) {
                setFetchingNotice(false);
                flowDeadline = 0L;
                statusText.setText("请完成学校官方登录；登录后将自动进入教务首页获取课表。");
            } else {
                if (flowDeadline == 0L) flowDeadline = SystemClock.elapsedRealtime() + ATTEMPT_TIMEOUT_MS;
                if (SystemClock.elapsedRealtime() > flowDeadline) {
                    event("自动获取超时", webView.getUrl());
                    failAttempt("当前通道获取超时", webView.getUrl());
                    return;
                }
                if (pending.isEmpty() && !extracting && !readingFile) navigate(generation);
            }
            handler.postDelayed(this, 1500L);
        }
    };
    // Also covers a main page that never finishes loading and a hanging download.
    private final Runnable attemptWatchdog = new Runnable() {
        @Override public void run() {
            if (!active || reviewing || flowStopped || localImport || !attempts.isRunning()) return;
            if (flowDeadline > 0L && SystemClock.elapsedRealtime() >= flowDeadline) {
                failAttempt("当前通道等待超时", webView == null ? null : webView.getUrl());
                return;
            }
            handler.postDelayed(this, 1000L);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_sync);
        browserContainer = findViewById(R.id.sync_browser_container);
        progressBar = findViewById(R.id.sync_progress);
        statusText = findViewById(R.id.sync_status);
        addressText = findViewById(R.id.sync_address);
        browserProblem = findViewById(R.id.sync_browser_problem);
        importButton = findViewById(R.id.import_current_page);
        try {
            navigationScript = asset("schedule-navigation.js");
            extractionScript = asset("schedule-extract.js");
        } catch (java.io.IOException error) {
            Toast.makeText(this, "同步组件加载失败，请重新安装 App", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        WebView.setWebContentsDebuggingEnabled(false);
        CookieManager.getInstance().setAcceptCookie(true);
        if (!getIntent().getBooleanExtra("manual_import", false)) {
            restartFlow();
        } else {
            statusText.setText("选择学校导出的 XLS；确认前不会覆盖原课表，也不会清除登录 Cookie。");
        }
        importButton.setOnClickListener(v -> restartFlow());
        findViewById(R.id.sync_close).setOnClickListener(v -> finish());
        findViewById(R.id.sync_logout).setOnClickListener(v -> confirmLogout());
        findViewById(R.id.sync_home).setOnClickListener(v -> restartFlow());
        findViewById(R.id.sync_diagnostics).setOnClickListener(v -> showDiagnostics());
        findViewById(R.id.sync_xls_license).setOnClickListener(v -> {
            try {
                TextView text = new TextView(this);
                text.setText(asset("xls-library-notice.txt") + "\n\n" + asset("LGPL-2.1.txt"));
                text.setTextSize(13);
                text.setPadding(dp(16), dp(10), dp(16), dp(10));
                ScrollView scroll = new ScrollView(this);
                scroll.addView(text);
                new AlertDialog.Builder(this).setTitle("XLS 组件许可")
                        .setView(scroll).setPositiveButton("关闭", null).show();
            } catch (java.io.IOException error) {
                Toast.makeText(this, "组件许可位于随附源码 third_party 文件夹", Toast.LENGTH_LONG).show();
            }
        });
        findViewById(R.id.sync_import_xls).setOnClickListener(v -> {
            if (reviewing || readingFile || loginReset.isClearing()) return;
            chooseManualFile();
        });
        if (getIntent().getBooleanExtra("manual_import", false) && savedInstanceState == null) chooseManualFile();
    }

    private void chooseManualFile() {
        Intent choose = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        choose.addCategory(Intent.CATEGORY_OPENABLE);
        choose.setType("*/*"); // The school may label XLS as application/octet-stream.
        try { startActivityForResult(choose, PICK_XLS); }
        catch (android.content.ActivityNotFoundException error) {
            Toast.makeText(this, "没有可用的文件选择器", Toast.LENGTH_LONG).show();
        }
    }

    private String asset(String name) throws java.io.IOException {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getAssets().open(name), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) text.append(line).append('\n');
        }
        return text.toString();
    }

    private void secureSettings(WebView view) {
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSafeBrowsingEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        // The SSO service is the school's PC portal; retain the installed Chromium version.
        java.util.regex.Matcher chrome = java.util.regex.Pattern.compile("Chrome/([0-9.]+)")
                .matcher(WebSettings.getDefaultUserAgent(this));
        if (chrome.find()) settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + chrome.group(1) + " Safari/537.36");
        CookieManager.getInstance().setAcceptThirdPartyCookies(view, true);
    }

    private WebView createWindow() {
        WebView view = new WebView(this);
        secureSettings(view);
        view.setVisibility(View.GONE);
        browserContainer.addView(view, new FrameLayout.LayoutParams(-1, -1));
        windows.add(view);
        configureWebView(view);
        return view;
    }

    private void configureWebView(WebView browser) {
        browser.setDownloadListener((url, agent, disposition, mime, length) -> {
            if (!active || reviewing || readingFile || flowStopped || !windows.contains(browser)) return;
            // The actual observed URL is the only export endpoint. Never store its query or Cookie.
            String sourcePage = downloadSourcePage(browser, url);
            if (!SchoolSyncPolicy.isAcademicDownload(url, sourcePage)) {
                event("阻止非教务HTTPS文件下载", url);
                failAttempt("导出地址不属于当前教务通道", url);
                return;
            }
            event("v.nua.edu.cn".equalsIgnoreCase(Uri.parse(url).getHost())
                    ? "接收学校外网网关XLS导出请求" : "接收学校XLS导出请求", url);
            beginFileRead(null, url, browser.getSettings().getUserAgentString(), sourcePage);
        });
        browser.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int progress) {
                if (view != webView || flowStopped) return;
                progressBar.setProgress(progress);
                progressBar.setVisibility(progress >= 100 ? View.GONE : View.VISIBLE);
            }
            @Override public boolean onCreateWindow(WebView view, boolean dialog,
                                                    boolean gesture, Message message) {
                if (!active || reviewing || flowStopped || !windows.contains(view) || windows.size() >= 6
                        || !trusted(view.getUrl())) return false;
                for (WebView child : pending) {
                    if (openers.get(child) == view) {
                        if (duplicateWindowNoted.add(view)) event("忽略同入口重复新窗口请求", view.getUrl());
                        return false;
                    }
                }
                WebView popup = createWindow();
                String portal = SchoolSyncPolicy.portalRetryFor(view.getUrl());
                if (!portal.isEmpty()) confirmedPortal = portal;
                pending.add(popup);
                openers.put(popup, view);
                // Do not stop, pause, or hide the opener: it may still submit into this window.
                event("等待学校新窗口加载", view.getUrl());
                statusText.setText("正在连接教务系统，请稍候…");
                ((WebView.WebViewTransport) message.obj).setWebView(popup);
                message.sendToTarget();
                handler.postDelayed(() -> {
                    if (pending.contains(popup)) {
                        event("新窗口未完成加载", popup.getUrl());
                        failAttempt("教务新窗口加载失败", popup.getUrl());
                    }
                }, 20000L);
                return true;
            }
            @Override public void onCloseWindow(WebView view) {
                if (!windows.contains(view)) return;
                WebView opener = openers.get(view);
                boolean wasActive = view == webView;
                disposeWindow(view);
                if (wasActive) {
                    if (opener != null && windows.contains(opener)) switchTo(opener);
                    else if (!windows.isEmpty()) switchTo(windows.get(0));
                    else failAttempt("教务窗口已关闭", null);
                }
            }
            @Override public boolean onConsoleMessage(ConsoleMessage message) {
                if (!windows.contains(browser)) return true;
                if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                    String body = message.message();
                    String category = body.contains("Mixed Content") || body.contains("ERR_CLEARTEXT_NOT_PERMITTED")
                            ? "网页包含被阻止的明文资源" : body.contains("CORS") || body.contains("Access-Control-Allow-Origin")
                            ? "网页跨域请求错误" : body.contains("TypeError") ? "网页TypeError"
                            : body.contains("SyntaxError") ? "网页SyntaxError" : "网页脚本错误";
                    event(category + "（未记录错误正文）", browser.getUrl());
                }
                return true;
            }
        });
        browser.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!windows.contains(view) || loginReset.isClearing()) return true;
                String url = request.getUrl().toString();
                // Hidden SSO frames commonly use about:blank. Never treat them as top-level navigation.
                if (!request.isForMainFrame()) {
                    boolean allow = url.startsWith("https:") || SchoolSyncPolicy.isBlankWindow(url) || "about:srcdoc".equals(url);
                    if (!allow) event("阻止子框架跳转 " + request.getMethod(), url);
                    return !allow;
                }
                if (trusted(url) || (SchoolSyncPolicy.isBlankWindow(url)
                        && (pending.contains(view) || (!pending.isEmpty() && trusted(view.getUrl()))))) return false;
                String secure = SchoolSyncPolicy.secureHttpFallback(url, request.getMethod());
                if (!secure.isEmpty()) {
                    SchoolSyncPolicy.SecureUpgradeGuard guard = secureFallbacks.computeIfAbsent(view,
                            key -> new SchoolSyncPolicy.SecureUpgradeGuard());
                    SchoolSyncPolicy.UpgradeResult decision = guard.register(secure);
                    if (decision == SchoolSyncPolicy.UpgradeResult.ALLOW) {
                        event("学校HTTP跳转改用HTTPS 第" + guard.getAttemptCount() + "次（不发送明文，"
                                + (request.isRedirect() ? "服务器重定向" : "页面导航") + "）", url);
                        handler.post(() -> { if (windows.contains(view) && !flowStopped) view.loadUrl(secure); });
                        return true;
                    }
                    if (decision == SchoolSyncPolicy.UpgradeResult.DUPLICATE) {
                        // Cancel this duplicate HTTP navigation only. Do not stop/destroy the window
                        // or invalidate an already queued HTTPS ticket callback.
                        event("忽略重复HTTP请求 第" + guard.getDuplicateCount() + "次，保留HTTPS连接（"
                                + (request.isRedirect() ? "服务器重定向" : "页面导航") + "）", url);
                        return true;
                    }
                    event("HTTPS入口转换或重复请求达到安全上限，停止入口循环", url);
                }
                blockedNavigation(view, url, request.getMethod());
                return true;
            }
            @Override public void onPageStarted(WebView view, String url, Bitmap icon) {
                if (!windows.contains(view)) return;
                if (flowStopped) { view.stopLoading(); return; }
                if (!SchoolSyncPolicy.isBlankWindow(url) && !trusted(url)) {
                    view.stopLoading();
                    blockedNavigation(view, url, "直接加载");
                    return;
                }
                event("加载学校页面", url);
                String seenPortal = SchoolSyncPolicy.portalRetryFor(url);
                if (!seenPortal.isEmpty()) confirmedPortal = seenPortal;
                if (SchoolSyncPolicy.isAcademicTransportPage(url)
                        || (!confirmedPortal.isEmpty() && canRead(url) && SchoolSyncPolicy.syncRoute(url) != SyncAttemptPolicy.Route.AUTO)) academicVisited = true;
                if (!canRead(url) && academicVisited && recoverSession("认证回跳，保留本轮会话重试")) return;
                SyncAttemptPolicy.Route route = SchoolSyncPolicy.syncRoute(url);
                attempts.observe(route);
                if (SchoolSyncPolicy.isAcademicTransportPage(url) && !attempts.accepts(route)) {
                    view.stopLoading();
                    failAttempt("学校未提供所选备用教务通道", url);
                    return;
                }
                if (!canRead(url) && !SchoolSyncPolicy.isBlankWindow(url)) flowDeadline = 0L;
                if (view != webView) return;
                generation++;
                extracting = false;
                handler.removeCallbacks(poll);
                updateAddress();
                browserProblem.setVisibility(View.GONE);
                if (!canRead(url)) setFetchingNotice(false);
                if (!canRead(url)) flowDeadline = 0L;
                else if (flowDeadline == 0L) flowDeadline = SystemClock.elapsedRealtime() + ATTEMPT_TIMEOUT_MS;
                if (trusted(url)) statusText.setText(statusForUrl(url));
            }
            @Override public void onPageCommitVisible(WebView view, String url) {
                if (flowStopped) return;
                if (pending.contains(view) && trusted(url)) switchTo(view);
            }
            @Override public void onPageFinished(WebView view, String url) {
                if (!windows.contains(view) || flowStopped) return;
                if (pending.contains(view) && trusted(url)) switchTo(view);
                if (view != webView) return;
                CookieManager.getInstance().flush();
                updateAddress();
                importButton.setEnabled(true);
                startPolling();
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request,
                                                   WebResourceError error) {
                if (!windows.contains(view)) return;
                if (request.isForMainFrame()) {
                    String failedUrl = request.getUrl().toString();
                    event("页面错误 " + error.getErrorCode(), failedUrl);
                    // CAS/portal pages can emit transient main-frame errors while following
                    // the school's SSO redirect. They are not a failed course-fetch attempt.
                    // Only an authenticated academic transport error consumes a route budget.
                    if (SchoolSyncPolicy.isAcademicTransportPage(failedUrl)) {
                        failAttempt("学校教务页面连接失败 " + error.getErrorCode(), failedUrl);
                    } else {
                        flowDeadline = 0L;
                        statusText.setText("学校登录页面暂时加载失败，正在等待重新跳转…");
                    }
                }
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                                       WebResourceResponse response) {
                if (!windows.contains(view)) return;
                if (!request.isForMainFrame() && request.getUrl().getPath() != null
                        && request.getUrl().getPath().endsWith("/favicon.ico")) return;
                event("HTTP " + response.getStatusCode(), request.getUrl().toString());
                if (request.isForMainFrame()) {
                    String failedUrl = request.getUrl().toString();
                    if (SchoolSyncPolicy.isAcademicTransportPage(failedUrl)) {
                        failAttempt("学校教务页面 HTTP " + response.getStatusCode(), failedUrl);
                    } else {
                        flowDeadline = 0L;
                        statusText.setText("学校登录页面返回临时错误，正在等待重新跳转…");
                    }
                }
            }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler errorHandler, SslError error) {
                if (!windows.contains(view)) { errorHandler.cancel(); return; }
                event("证书校验失败（未绕过）", error.getUrl());
                errorHandler.cancel();
                failAttempt("学校证书校验失败（不绕过）", error.getUrl());
            }
            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                if (windows.contains(view) && !recovering) {
                    recovering = true;
                    event("网页渲染进程退出", view.getUrl());
                    handler.post(() -> {
                        if (!isFinishing() && active) failAttempt("网页渲染失败", view.getUrl());
                        recovering = false;
                    });
                }
                return true;
            }
        });
    }

    private static boolean trusted(String url) { return SchoolSyncPolicy.trusted(url); }
    private static boolean canRead(String url) { return SchoolSyncPolicy.canRead(url); }

    private void blockedNavigation(WebView view, String url, String method) {
        event("跳转被拦截 " + method, url);
        failAttempt("教务入口跳转被拦截", url);
    }

    private void failAttempt(String reason, String url) {
        if (localImport || reviewing || isFinishing() || isDestroyed()) return;
        if ((reason.contains("空白") || reason.contains("教务会话")) && recoverSession(reason)) return;
        SyncAttemptPolicy.Next next = attempts.fail();
        if (next == SyncAttemptPolicy.Next.IGNORE) return;
        event("通道失败 " + attempts.failures() + "/2：" + reason, url);
        flowStopped = true;
        generation++;
        handler.removeCallbacks(poll);
        handler.removeCallbacks(attemptWatchdog);
        cancelFileRead();
        for (WebView view : new ArrayList<>(windows)) disposeWindow(view);
        updateAddress();
        importButton.setEnabled(true);
        if (next == SyncAttemptPolicy.Next.STOP) {
            event("两个通道均未完成获取，停止本轮", null);
            statusText.setText(R.string.sync_failed_message);
            showBrowserProblem(getString(R.string.sync_failed_message) + "\n原课表保持不变，可查看同步诊断。");
            failureNoticePending = true;
            showFailureNotice();
            return;
        }
        String entry = next == SyncAttemptPolicy.Next.VPN ? VPN_ENTRY : DIRECT_ENTRY;
        statusText.setText(next == SyncAttemptPolicy.Next.VPN
                ? "当前通道失败，尝试外网网关（最后一次）…" : "当前通道失败，尝试直连门户（最后一次）…");
        final int token = generation;
        handler.post(() -> {
            if (token != generation || isFinishing() || isDestroyed()) return;
            if (!attempts.startFallback()) return;
            academicLoginCount = blankCount = profileRecoveries = 0;
            academicHomeNoted = false;
            event("启动备用通道：" + (next == SyncAttemptPolicy.Next.VPN ? "外网网关" : "直连门户"), entry);
            // Same user-initiated sync session; preserve this login, never replay an SSO ticket.
            resetBrowser(false, entry);
        });
    }

    private void showFailureNotice() {
        if (!active || !failureNoticePending || failureDialog != null || isFinishing() || isDestroyed()) return;
        failureNoticePending = false;
        failureDialog = new AlertDialog.Builder(this).setTitle("获取失败")
                .setMessage(getString(R.string.sync_failed_message) + "\n\n也可以在学校网页导出 XLS，再手动导入；确认前原课表保持不变。")
                .setPositiveButton("手动导入 XLS", (dialog, which) -> chooseManualFile())
                .setNegativeButton("稍后重试", null).create();
        failureDialog.setOnDismissListener(dialog -> failureDialog = null);
        failureDialog.show();
    }

    private String statusForUrl(String url) {
        if (SchoolSyncPolicy.isAcademicHome(url)) return "正在检查教务会话：培养管理 → 我的课表 → 学期课表；不使用教务密码直登。";
        return canRead(url) ? "登录后自动获取：教务首页 → 培养管理 → 我的课表 → 学期课表"
                : "请完成学校官方登录；App 不读取密码，登录后自动获取课表。";
    }

    private void updateAddress() {
        addressText.setText(SchoolSyncPolicy.displayAddress(webView == null ? null : webView.getUrl()));
    }

    private void showBrowserProblem(String text) {
        setFetchingNotice(false);
        browserProblem.setText(text);
        browserProblem.setVisibility(View.VISIBLE);
        browserProblem.bringToFront();
        progressBar.setVisibility(View.GONE);
    }

    private void setFetchingNotice(boolean fetching) {
        fetchingNoticeActive = fetching;
        if (!fetching || !active || reviewing || flowStopped || isFinishing() || isDestroyed()) {
            dismissFetchingDialog();
            return;
        }
        if (fetchingNoticeHidden || fetchingDialog != null) return;
        LinearLayout body = new LinearLayout(this);
        body.setGravity(android.view.Gravity.CENTER_VERTICAL);
        body.setPadding(dp(24), dp(20), dp(24), dp(8));
        ProgressBar spinner = new ProgressBar(this);
        body.addView(spinner, new LinearLayout.LayoutParams(dp(32), dp(32)));
        TextView message = new TextView(this);
        message.setText(R.string.sync_fetching_message);
        message.setTextSize(16);
        message.setPadding(dp(16), 0, 0, 0);
        body.addView(message, new LinearLayout.LayoutParams(0, -2, 1));
        fetchingDialog = new AlertDialog.Builder(this)
                .setView(body)
                .setPositiveButton(R.string.sync_hide_fetching, (dialog, which) -> {
                    // Hiding the notice must not cancel navigation, download or parsing.
                    fetchingNoticeHidden = true;
                    dismissFetchingDialog();
                }).create();
        fetchingDialog.setCanceledOnTouchOutside(false);
        fetchingDialog.setOnCancelListener(dialog -> {
            fetchingNoticeHidden = true;
            dismissFetchingDialog();
        });
        fetchingDialog.show();
    }

    private void dismissFetchingDialog() {
        if (fetchingDialog == null) return;
        AlertDialog dialog = fetchingDialog;
        fetchingDialog = null;
        dialog.dismiss();
    }

    private void switchTo(WebView view) {
        if (!windows.contains(view)) return;
        pending.remove(view);
        for (WebView window : windows) window.setVisibility(window == view ? View.VISIBLE : View.GONE);
        webView = view;
        generation++;
        extracting = false;
        blankCount = 0;
        browserProblem.setVisibility(View.GONE);
        updateAddress();
        statusText.setText(statusForUrl(view.getUrl()));
        startPolling();
    }

    private void startPolling() {
        handler.removeCallbacks(poll);
        if (active && !reviewing && !flowStopped) handler.postDelayed(poll, 1000L);
    }

    private void prepareRetry() {
        cancelFileRead();
        localImport = false;
        confirmedPortal = "";
        academicVisited = sessionRecovered = false;
        attempts.reset();
        handler.removeCallbacks(attemptWatchdog);
        failureNoticePending = false;
        if (failureDialog != null) failureDialog.dismiss();
        fetchingNoticeHidden = false;
        generation++;
        handler.removeCallbacks(poll);
        extracting = false;
        flowStopped = false;
        blankCount = 0;
        academicLoginCount = 0;
        profileRecoveries = 0;
        academicHomeNoted = false;
        flowDeadline = 0L;
        lastPreviewJson = "";
        lastPageStructure = "";
        browserProblem.setVisibility(View.GONE);
    }

    private void restartFlow() { restartFlow(false); }

    private boolean recoverSession(String reason) {
        if (localImport || flowStopped || sessionRecovered || confirmedPortal.isEmpty() || !academicVisited) return false;
        sessionRecovered = true;
        event("保留首次登录 Cookie，同通道重新获取一次（不清空登录、不复用票据）", null);
        statusText.setText("首次取课中断，正在保留登录会话并自动重试…");
        handler.removeCallbacks(poll);
        generation++;
        final int token=generation;
        handler.postDelayed(()->{if(token==generation&&!isFinishing()&&!isDestroyed())resetBrowser(false,confirmedPortal);},900);
        return true;
    }

    private void restartFlow(boolean forceClear) {
        if (loginReset.isClearing() || isFinishing() || isDestroyed()) return;
        if (reviewDialog != null) reviewDialog.dismiss();
        prepareRetry();
        flowStopped = true;
        setSessionControls(false);
        statusText.setText("正在清空旧登录 Cookie 和网页缓存；原课表保持不变…");
        event("重新获取：清除旧登录并取消旧页面/下载", null);
        // Stop and destroy every old window before removal, so old scripts cannot repopulate cookies.
        for (WebView view : new ArrayList<>(windows)) {
            view.stopLoading();
            view.clearCache(true);
            view.clearHistory();
            view.clearFormData();
            disposeWindow(view);
        }
        openers.clear();
        updateAddress();
        WebStorage.getInstance().deleteAllData();
        WebViewDatabase database = WebViewDatabase.getInstance(this);
        database.clearFormData();
        database.clearHttpAuthUsernamePassword();
        final int token = loginReset.begin();
        CookieManager manager = CookieManager.getInstance();
        manager.removeAllCookies(removed -> {
            if (isFinishing() || isDestroyed()) return;
            // 'removed' is false when there were no cookies; it is not a failure flag.
            boolean empty = !manager.hasCookies();
            if (!loginReset.complete(token, empty)) return;
            manager.flush();
            if (!empty) {
                setSessionControls(true);
                event("旧登录仍未清空，未加载新入口", null);
                statusText.setText("登录 Cookie 未能清空，原课表未改变，请再次点击重新获取。");
                return;
            }
            event("旧登录Cookie已清空，等待重新登录", null);
            openFreshLoginIfReady();
        });
    }

    private void setSessionControls(boolean enabled) {
        importButton.setEnabled(enabled);
        findViewById(R.id.sync_home).setEnabled(enabled);
        findViewById(R.id.sync_logout).setEnabled(enabled);
        findViewById(R.id.sync_import_xls).setEnabled(enabled);
    }

    private void openFreshLoginIfReady() {
        if (isFinishing() || isDestroyed() || !loginReset.takeReady(active)) return;
        setSessionControls(true);
        resetBrowser(true, PORTAL_URL);
        statusText.setText("旧登录已清空，请重新完成学校官方登录；登录后自动获取课表。");
    }

    private void navigate(int token) {
        if (!active || reviewing || readingFile || flowStopped || token != generation || webView == null || !canRead(webView.getUrl())) return;
        if (SchoolSyncPolicy.isPersonalSettings(webView.getUrl())) {
            // Restore the confirmed shell once; never read, search or submit a profile-edit form.
            if (profileRecoveries++ == 0) {
                event("检测到个人信息页，返回教务首页一次（不操作表单）", webView.getUrl());
                statusText.setText("正在返回教务首页，继续获取学期课表…");
                String home = SchoolSyncPolicy.academicHomeFor(webView.getUrl());
                if (home.isEmpty()) { failAttempt("未能识别当前教务首页", webView.getUrl()); return; }
                webView.loadUrl(home);
            } else {
                event("学校仍返回个人信息页，停止自动返回", webView.getUrl());
                failAttempt("学校仍显示个人信息页", webView.getUrl());
            }
            return;
        }
        extracting = true;
        webView.evaluateJavascript(navigationScript, value -> {
            if (!active || flowStopped || token != generation || webView == null) return;
            extracting = false;
            try {
                JSONObject action = new JSONObject(decode(value));
                String kind = action.optString("kind", "wait");
                String label = action.optString("action", "");
                setFetchingNotice(!"login".equals(kind) && !"sso_required".equals(kind));
                statusText.setText("自动获取课表 · " + label);
                if ("export".equals(kind) || "export_wait".equals(kind)) {
                    if (exportWaitUntil == 0L) {
                        exportWaitUntil = SystemClock.elapsedRealtime() + 12000L;
                        event("优先等待课表XLS导出", webView.getUrl());
                    } else if (!readingFile && SystemClock.elapsedRealtime() >= exportWaitUntil) {
                        event("未收到有效导出请求，改用页面读取（可导入下载文件）", webView.getUrl());
                        enablePageFallback();
                    }
                    return;
                }
                JSONObject structure = action.optJSONObject("structure");
                if (structure != null && ("wait".equals(kind) || "ready".equals(kind)
                        || "export".equals(kind) || "export_wait".equals(kind))) {
                    String summary = "取课阶段：页面=" + Math.max(0, Math.min(30, structure.optInt("documents")))
                            + "，不可读框架=" + Math.max(0, Math.min(30, structure.optInt("frameErrors")))
                            + "，表格=" + Math.max(0, Math.min(1000, structure.optInt("tables")))
                            + "，课程列=" + structure.optBoolean("courseHeader")
                            + "，时间列=" + structure.optBoolean("timeHeader")
                            + "，隐藏框架=" + Math.max(0, Math.min(30, structure.optInt("hiddenFrames")))
                            + "，本轮导出控件=" + Math.max(0, Math.min(30, structure.optInt("exportControls")));
                    if (!summary.equals(lastPageStructure)) {
                        lastPageStructure = summary;
                        event(summary, webView.getUrl());
                    }
                }
                if ("sso_required".equals(kind)) {
                    blankCount = 0;
                    // CAS can briefly expose the academic login form while the
                    // just-created portal Cookie is still being copied to jwnew.
                    // Do not switch routes (and make the user log in again) on
                    // that transient state; wait a bounded ~9 seconds first.
                    if (++academicLoginCount >= 6) {
                        failAttempt("教务会话等待超时（不提交教务密码）", webView.getUrl());
                    }
                    return;
                }
                academicLoginCount = 0;
                if (action.optBoolean("academicHome") && !academicHomeNoted) {
                    academicHomeNoted = true;
                    event("识别教务首页菜单，查找培养管理→我的课表→学期课表", webView.getUrl());
                }
                if ("blank".equals(kind)) {
                    // A portal/SSO frame can report an empty DOM several times while its
                    // redirect and WebView document are still being committed. Six polls
                    // (~9 seconds) is the route-level blank failure threshold.
                    if (++blankCount >= 6) {
                        event("学校页面持续空白", webView.getUrl());
                        failAttempt("学校页面连续两次空白", webView.getUrl());
                    }
                } else if ("open".equals(kind) && SchoolSyncPolicy.canAutoOpenFrame(webView.getUrl(), action.optString("url"))) {
                    blankCount = 0;
                    event("进入页面内教务框架", action.optString("url"));
                    webView.loadUrl(action.optString("url"));
                } else {
                    blankCount = 0;
                    if ("acted".equals(kind)) event(label, webView.getUrl());
                    else if ("ready".equals(kind) || "wait".equals(kind)) extractSchedule(false);
                }
            } catch (JSONException error) {
                event("菜单脚本返回格式错误", webView.getUrl());
                failAttempt("自动菜单识别失败", webView.getUrl());
            }
        });
    }

    private void extractSchedule(boolean requested) {
        if (!active || flowStopped || readingFile || exportWaitUntil > 0 || webView == null || extracting || reviewing
                || !canRead(webView.getUrl()) || SchoolSyncPolicy.isPersonalSettings(webView.getUrl())) return;
        extracting = true;
        final int token = generation;
        webView.evaluateJavascript(extractionScript, value -> {
            if (token != generation || !active || flowStopped || webView == null) return;
            extracting = false;
            String json = decode(value);
            if (json.isEmpty()) return;
            if (!requested && json.equals(lastPreviewJson)) return;
            lastPreviewJson = json;
            try {
                OfficialScheduleParser.ParseResult result = OfficialScheduleParser.parse(json);
                event("自动识别课程 " + result.getCourseCount() + " 门", webView.getUrl());
                showPreview(result, new JSONObject(json));
            } catch (JSONException | RuntimeException error) {
                event("课表解析失败（未覆盖原数据）", webView.getUrl());
                failAttempt("网页课表格式未识别", webView.getUrl());
            }
        });
    }

    private void enablePageFallback() {
        exportWaitUntil = 0L;
        // One short DOM fallback, not another network attempt or an indefinite wait.
        flowDeadline = Math.min(flowDeadline == 0L ? Long.MAX_VALUE : flowDeadline,
                SystemClock.elapsedRealtime() + 8000L);
        if (webView != null) webView.evaluateJavascript("window.__nuaAllowPageFallback = true;", null);
        startPolling();
    }

    private void cancelFileRead() {
        setFetchingNotice(false);
        fileGeneration++;
        if (fileTask != null) fileTask.cancel(true);
        fileTask = null;
        readingFile = false;
        pendingFileResult = null;
        exportWaitUntil = 0L;
    }

    private String downloadSourcePage(WebView browser, String downloadUrl) {
        // A download popup may still be about:blank. Prefer the current page, then its opener.
        if (webView != null && SchoolSyncPolicy.isAcademicDownload(downloadUrl, webView.getUrl()))
            return webView.getUrl();
        for (int depth = 0; browser != null && depth < 6; depth++, browser = openers.get(browser)) {
            if (SchoolSyncPolicy.isAcademicDownload(downloadUrl, browser.getUrl())) return browser.getUrl();
        }
        return null;
    }

    private void beginFileRead(Uri selected, String downloadUrl, String userAgent, String sourcePage) {
        cancelFileRead();
        localImport = selected != null;
        if (localImport) handler.removeCallbacks(attemptWatchdog);
        readingFile = true;
        setFetchingNotice(true);
        browserProblem.setVisibility(View.GONE);
        handler.removeCallbacks(poll);
        statusText.setText(selected == null ? "正在安全下载并读取学校 XLS…" : "正在读取所选教务 XLS…");
        importButton.setEnabled(false);
        final int token = fileGeneration;
        fileTask = fileWorker.submit(() -> {
            try {
                byte[] bytes;
                if (selected == null) bytes = AcademicExportDownload.fetch(downloadUrl, userAgent,
                        url -> CookieManager.getInstance().getCookie(url), sourcePage,
                        (summary, url) -> handler.post(() -> {
                            if (token == fileGeneration && !isFinishing() && !isDestroyed()) event(summary, url);
                        }));
                else try (InputStream input = getContentResolver().openInputStream(selected)) {
                    if (input == null) throw new java.io.IOException("无法打开文件");
                    bytes = ExportedXlsReader.readBounded(input);
                }
                final int byteCount = bytes.length;
                handler.post(() -> {
                    if (token == fileGeneration && !isFinishing() && !isDestroyed())
                        event("收到待校验文件：" + byteCount + "字节（未记录正文）", null);
                });
                OfficialScheduleParser.ParseResult result = ExportedXlsReader.parse(bytes);
                handler.post(() -> completeFileRead(token, () -> {
                    readingFile = false;
                    setFetchingNotice(false);
                    importButton.setEnabled(true);
                    exportWaitUntil = 0L;
                    event("XLS识别完成：" + result.getCourseCount() + "门课程，" + result.getRules().size() + "次上课", null);
                    try { showPreview(result, new JSONObject().put("format", "exported-xls")); }
                    catch (JSONException | RuntimeException error) {
                        reviewing = false;
                        if (selected == null) failAttempt("导入预览未能打开", null);
                        else statusText.setText("导入预览未能打开，原课表未改变，请重试。");
                    }
                }));
            } catch (java.io.IOException | RuntimeException error) {
                final String category = error instanceof javax.net.ssl.SSLException ? "证书或TLS连接失败"
                        : error instanceof java.net.SocketTimeoutException ? "学校下载超时" : "文件下载或格式未识别";
                handler.post(() -> completeFileRead(token, () -> {
                    readingFile = false;
                    setFetchingNotice(false);
                    importButton.setEnabled(true);
                    event("XLS导入失败：" + category + "，错误类型=" + error.getClass().getSimpleName()
                            + "（未记录错误或文件正文）", null);
                    if (selected == null) failAttempt("XLS下载或读取失败：" + category, null);
                    else {
                        statusText.setText("XLS 未能导入，原课表保持不变。可重新选择学校导出的文件。");
                        Toast.makeText(this, "XLS 未能读取：" + category + "。原课表未改变。", Toast.LENGTH_LONG).show();
                    }
                }));
            }
        });
    }

    private void completeFileRead(int token, Runnable result) {
        if (token != fileGeneration || isFinishing() || isDestroyed()) return;
        if (!active) { pendingFileResult = result; return; }
        result.run();
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_XLS || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (!"content".equals(uri.getScheme())) {
            Toast.makeText(this, "请通过文件选择器选择学校导出的 XLS", Toast.LENGTH_LONG).show();
            return;
        }
        flowStopped = false;
        beginFileRead(uri, null, null, null);
    }

    private void showPreview(OfficialScheduleParser.ParseResult result, JSONObject metadata) {
        setFetchingNotice(false);
        if (result.getRules().isEmpty()) {
            if (!localImport) failAttempt("课表未包含可导入的课程", null);
            else statusText.setText("文件未包含可导入的课程，原课表保持不变。");
            return;
        }
        reviewing = true;
        handler.removeCallbacks(poll);
        LocalDate[] startDate = { ScheduleRepository.schoolStartDate() };
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(10), dp(20), dp(10));
        StringBuilder summary = new StringBuilder();
        String semester = metadata.optString("semester", "");
        if (!semester.isEmpty()) summary.append(semester).append("\n\n");
        summary.append(result.getCourseCount()).append(" 门课程 · ")
                .append(result.getRules().size()).append(" 次上课\n");
        if (result.getSkippedCount() > 0) summary.append("已排除军事技能。\n");
        summary.append("\n").append(String.join("、", result.getCourses()));
        int max = 1;
        for (cn.nua.wenguan.schedule.model.ScheduleRule rule : result.getRules()) max = Math.max(max, rule.getToWeek());
        summary.append("\n\n导入后使用第 1—").append(max).append(" 周范围。请按校历选择开学时间，所选日期所在周设为第1周，并核对")
                .append("exported-xls".equals(metadata.optString("format")) ? "导出文件的学期和课程是否完整。" : "网页已显示完整课表。");
        TextView info = new TextView(this);
        info.setText(summary);
        info.setTextSize(15);
        body.addView(info);
        Button dateButton = new Button(this);
        dateButton.setText("开学时间：" + startDate[0]);
        TextView weekHint = new TextView(this);
        weekHint.setText(ScheduleRepository.firstWeekText(startDate[0]));
        weekHint.setTextSize(13);
        dateButton.setOnClickListener(v -> {
            DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
                startDate[0] = LocalDate.of(year, month + 1, day);
                dateButton.setText("开学时间：" + startDate[0]);
                weekHint.setText(ScheduleRepository.firstWeekText(startDate[0]));
            }, startDate[0].getYear(), startDate[0].getMonthValue() - 1, startDate[0].getDayOfMonth());
            picker.setTitle("选择开学时间 · 所在周为第1周");
            picker.show();
        });
        body.addView(dateButton);
        body.addView(weekHint);
        List<String> warnings = new ArrayList<>(result.getWarnings());
        JSONArray pageWarnings = metadata.optJSONArray("warnings");
        if (pageWarnings != null) for (int i = 0; i < pageWarnings.length(); i++) warnings.add(pageWarnings.optString(i));
        CheckBox checked = new CheckBox(this);
        if (!warnings.isEmpty()) {
            TextView warning = new TextView(this);
            warning.setText("需要核对：\n" + String.join("\n", warnings));
            warning.setTextColor(0xFFAA4622);
            warning.setTextSize(14);
            body.addView(warning);
            checked.setText("已核对以上提示，仍要导入识别出的课程");
            body.addView(checked);
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        reviewDialog = new AlertDialog.Builder(this).setTitle("确认课表 · 不会立即覆盖")
                .setView(scroll).setNegativeButton("取消", null).setPositiveButton("确认导入", null).create();
        reviewDialog.setOnDismissListener(dialog -> {
            reviewing = false;
            reviewDialog = null;
            statusText.setText("已取消导入；点“重新获取课表”会清空登录并重新认证。");
        });
        reviewDialog.setOnShowListener(dialog -> reviewDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (!warnings.isEmpty() && !checked.isChecked()) {
                Toast.makeText(this, "请先核对并勾选提示", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                ScheduleRuleStore.save(this, result.getRules(), "南京艺术学院教务系统", startDate[0]);
                refreshWidget();
                setResult(RESULT_OK);
                Toast.makeText(this, "课表已导入，小组件已更新", Toast.LENGTH_LONG).show();
                reviewDialog.dismiss();
                finish();
            } catch (JSONException | RuntimeException error) {
                Toast.makeText(this, "保存失败，原课表未被替换：" + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }));
        reviewDialog.show();
        attempts.succeed();
        handler.removeCallbacks(attemptWatchdog);
        scroll.getLayoutParams().height = Math.min(dp(420), getResources().getDisplayMetrics().heightPixels / 2);
    }


    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private String decode(String value) {
        if (value == null || value.equals("null") || value.equals("\"\"")) return "";
        try { return new JSONArray("[" + value + "]").getString(0); }
        catch (JSONException ignored) { return ""; }
    }

    private void refreshWidget() {
        Intent intent = new Intent(this, TodayScheduleWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        ComponentName component = new ComponentName(this, TodayScheduleWidgetProvider.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, manager.getAppWidgetIds(component));
        sendBroadcast(intent);
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this).setTitle("退出学校登录")
                .setMessage("清除本 App 网页登录 Cookie、缓存及网页本地存储，不删除已导入课表。")
        .setNegativeButton("取消", null).setPositiveButton("退出登录", (dialog, which) -> restartFlow(true)).show();
    }

    private void disposeWindow(WebView view) {
        pending.remove(view);
        openers.remove(view);
        secureFallbacks.remove(view);
        duplicateWindowNoted.remove(view);
        if (!windows.remove(view)) return;
        if (view == webView) webView = null;
        view.stopLoading();
        view.setWebChromeClient(null);
        view.setWebViewClient(null);
        view.setDownloadListener(null);
        browserContainer.removeView(view);
        view.destroy();
    }

    private void resetBrowser(boolean clearCache) { failAttempt("当前教务窗口未能继续", null); }

    private void resetBrowser(boolean clearCache, String startUrl) {
        if (loginReset.isClearing() || isFinishing() || isDestroyed()) return;
        cancelFileRead();
        generation++;
        extracting = false;
        flowDeadline = SystemClock.elapsedRealtime() + ATTEMPT_TIMEOUT_MS;
        flowStopped = false;
        blankCount = 0;
        academicLoginCount = 0;
        lastPageStructure = "";
        browserProblem.setVisibility(View.GONE);
        lastPreviewJson = "";
        handler.removeCallbacks(poll);
        for (WebView view : new ArrayList<>(windows)) disposeWindow(view);
        openers.clear();
        webView = createWindow();
        webView.setVisibility(View.VISIBLE);
        if (clearCache) webView.clearCache(true);
        boolean resume = ACADEMIC_HOME.equals(startUrl);
        event(resume ? "当前同步流程连接教务首页" : "连接学校入口（当前同步流程）", startUrl);
        updateAddress();
        statusText.setText(resume ? "正在检查已有教务会话，失效后自动返回门户登录…" : "正在连接学校登录入口…");
        webView.loadUrl(startUrl);
        handler.removeCallbacks(attemptWatchdog);
        if (active) handler.postDelayed(attemptWatchdog, 1000L);
    }

    private void event(String action, String url) {
        String entry = java.time.LocalTime.now(ScheduleRepository.SCHOOL_ZONE)
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss", java.util.Locale.ROOT))
                + " · " + action + " · " + SchoolSyncPolicy.displayAddress(url);
        if (diagnostics.isEmpty() || !diagnostics.get(diagnostics.size() - 1).equals(entry)) diagnostics.add(entry);
        if (diagnostics.size() > 60) diagnostics.remove(0);
    }

    private void showDiagnostics() {
        android.content.pm.PackageInfo webPackage = WebView.getCurrentWebViewPackage();
        String webVersion = webPackage == null ? "未知" : webPackage.versionName;
        String appVersion;
        try { appVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (android.content.pm.PackageManager.NameNotFoundException ignored) { appVersion = "未知"; }
        String text = getString(R.string.app_name) + " " + appVersion + "\nAndroid " + android.os.Build.VERSION.RELEASE + "\nWebView " + webVersion
                + "\n" + String.join("\n", diagnostics)
                + "\n仅含状态、错误类型与脱敏页面路径；不含账号密码、Cookie、登录票据或页面正文。";
        new AlertDialog.Builder(this).setTitle("同步诊断")
                .setMessage(text).setNegativeButton("关闭", null)
                .setPositiveButton("复制", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("同步诊断", text));
                    Toast.makeText(this, "诊断已复制", Toast.LENGTH_SHORT).show();
                }).show();
    }

    @Override protected void onResume() {
        super.onResume();
        active = true;
        if (pausedAt > 0L && flowDeadline > 0L)
            flowDeadline += SystemClock.elapsedRealtime() - pausedAt;
        pausedAt = 0L;
        openFreshLoginIfReady();
        for (WebView view : windows) view.onResume();
        if (pendingFileResult != null) {
            Runnable result = pendingFileResult;
            pendingFileResult = null;
            result.run();
        }
        setFetchingNotice(fetchingNoticeActive);
        showFailureNotice();
        handler.removeCallbacks(attemptWatchdog);
        if (attempts.isRunning() && !localImport) handler.postDelayed(attemptWatchdog, 1000L);
        startPolling();
    }
    @Override protected void onPause() {
        active = false;
        pausedAt = SystemClock.elapsedRealtime();
        dismissFetchingDialog();
        if (failureDialog != null) { failureNoticePending = true; failureDialog.dismiss(); }
        generation++;
        extracting = false;
        handler.removeCallbacks(poll);
        handler.removeCallbacks(attemptWatchdog);
        for (WebView view : windows) view.onPause();
        super.onPause();
    }
    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else if (webView != null && openers.containsKey(webView)) {
            WebView current = webView;
            WebView opener = openers.get(current);
            disposeWindow(current);
            if (windows.contains(opener)) switchTo(opener); else resetBrowser(false);
        } else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        active = false;
        dismissFetchingDialog();
        if (failureDialog != null) failureDialog.dismiss();
        loginReset.invalidate();
        cancelFileRead();
        fileWorker.shutdownNow();
        handler.removeCallbacksAndMessages(null);
        if (reviewDialog != null) reviewDialog.dismiss();
        for (WebView view : new ArrayList<>(windows)) disposeWindow(view);
        // End the temporary official login session; retention is no longer a feature.
        if (!getIntent().getBooleanExtra("manual_import", false)) {
            try { CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush(); }
            catch (RuntimeException ignored) { }
        }
        super.onDestroy();
    }
}
