import cn.nua.wenguan.schedule.data.SchoolSyncPolicy;

public final class ValidateSchoolSyncPolicy {
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        require(SchoolSyncPolicy.trusted("https://i.nua.edu.cn/sopcb/"), "官方门户");
        require(SchoolSyncPolicy.canRead("https://jwnew.nua.edu.cn/jsxsd/"), "教务系统");
        require(SchoolSyncPolicy.isAcademicHome("https://jwnew.nua.edu.cn/jsxsd/framework/xsMain.jsp?ignored=1"), "用户确认的首页地址（不代表会话有效）");
        require(!SchoolSyncPolicy.isAcademicHome("https://i.nua.edu.cn/jsxsd/framework/xsMain.jsp"), "其他域名不能冒充教务首页");
        require(!SchoolSyncPolicy.isAcademicHome("http://jwnew.nua.edu.cn/jsxsd/framework/xsMain.jsp"), "教务首页必须为HTTPS");
        require(SchoolSyncPolicy.isAcademicPage("https://jwnew.nua.edu.cn/jsxsd/framework/xsMain.jsp"), "当前教务页可原地重试");
        require(!SchoolSyncPolicy.isAcademicPage("https://jwnew.nua.edu.cn/sso.jsp?ticket=ST-test-only"), "重试不能重放SSO票据页");
        require(!SchoolSyncPolicy.isAcademicPage("https://c.nua.edu.cn/cas/login"), "重试不能在CAS注入导航");
        require(!SchoolSyncPolicy.isAcademicPage("https://i.nua.edu.cn/jsxsd/framework/xsMain.jsp"), "不能把门户当成当前教务页");
        require(!SchoolSyncPolicy.canRead("https://c.nua.edu.cn/cas/login"), "不在CAS运行脚本");
        require(!SchoolSyncPolicy.canRead("https://v.nua.edu.cn/https/school/cas/login?service=xx"), "VPN内CAS也受保护");
        require(!SchoolSyncPolicy.trusted("https://fake-nua.edu.cn/"), "仿冒域名");
        require(!SchoolSyncPolicy.trusted("https://nua.edu.cn.attacker.test/"), "后缀仿冒域名");
        require(!SchoolSyncPolicy.trusted("http://i.nua.edu.cn/"), "不放开明文登录");
        require(!SchoolSyncPolicy.trusted("about:blank"), "空白新窗口不视为加载完成");
        require(!SchoolSyncPolicy.trusted(null), "未加载URL");
        require(!SchoolSyncPolicy.trusted("https://user:password@i.nua.edu.cn/"), "拒绝带账户信息的地址");
        require(SchoolSyncPolicy.isBlankWindow("about:blank#state"), "空窗口允许片段但不视为完成页面");
        require(!SchoolSyncPolicy.isBlankWindow("javascript:window.open()"), "脚本地址不是空窗口");
        String upgraded = SchoolSyncPolicy.secureHttpFallback("http://jwnew.nua.edu.cn:80/jsxsd/?service=https%3A%2F%2Fi.nua.edu.cn#page", "GET");
        require(upgraded.equals("https://jwnew.nua.edu.cn/jsxsd/?service=https%3A%2F%2Fi.nua.edu.cn#page"), "安全升级保持编码、片段，不把参数二次编码");
        require(SchoolSyncPolicy.secureHttpFallback("http://jwnew.nua.edu.cn/", "POST").isEmpty(), "不重放POST正文");
        require(SchoolSyncPolicy.secureHttpFallback("http://outside.example/", "GET").isEmpty(), "不扩大到其他域名");
        require(SchoolSyncPolicy.secureHttpFallback("http://jwnew.nua.edu.cn:8080/", "GET").isEmpty(), "不猜测自定义端口的TLS服务");
        require(SchoolSyncPolicy.secureHttpFallback("http://user:password@jwnew.nua.edu.cn/", "GET").isEmpty(), "不升级URL内密码");
        SchoolSyncPolicy.SecureUpgradeGuard guard = new SchoolSyncPolicy.SecureUpgradeGuard();
        String entry = SchoolSyncPolicy.secureHttpFallback("http://jwnew.nua.edu.cn/sso.jsp", "GET");
        String callback = SchoolSyncPolicy.secureHttpFallback("http://jwnew.nua.edu.cn/sso.jsp?ticket=ST-test-only%2Babc", "GET");
        require(guard.register(entry) == SchoolSyncPolicy.UpgradeResult.ALLOW, "首次SSO入口安全升级");
        require(guard.register(callback) == SchoolSyncPolicy.UpgradeResult.ALLOW, "同路径携带票据的CAS回调是不同请求，不能误拦截");
        require(guard.register(callback + "#other") == SchoolSyncPolicy.UpgradeResult.DUPLICATE, "仅改变片段不能绕过防循环");
        require(guard.register("https://JWNEW.NUA.EDU.CN:443/sso.jsp") == SchoolSyncPolicy.UpgradeResult.DUPLICATE, "域名大小写和默认端口不能绕过重复请求判断");
        require(guard.register(callback) == SchoolSyncPolicy.UpgradeResult.DUPLICATE, "同一票据不可重复请求");
        require(guard.register("https://jwnew.nua.edu.cn/sso.jsp?ticket=ST-test-only%252Babc") == SchoolSyncPolicy.UpgradeResult.ALLOW, "原始编码不同的查询参数不能折叠");
        require(guard.getAttemptCount() == 3, "重复请求不消耗新请求名额");
        for (int i = 3; i < SchoolSyncPolicy.SecureUpgradeGuard.MAX_ATTEMPTS; i++) {
            require(guard.register("https://jwnew.nua.edu.cn/sso.jsp?step=" + i) == SchoolSyncPolicy.UpgradeResult.ALLOW, "不同请求在上限内可转换");
        }
        require(guard.register("https://jwnew.nua.edu.cn/sso.jsp?step=999") == SchoolSyncPolicy.UpgradeResult.LIMIT, "不同参数也不能无限重试");
        require(guard.register("http://jwnew.nua.edu.cn/sso.jsp") == SchoolSyncPolicy.UpgradeResult.INVALID, "guard仅接受HTTPS目标");
        require(guard.register("https://outside.example/sso.jsp") == SchoolSyncPolicy.UpgradeResult.INVALID, "guard不扩大到校外地址");
        require(new SchoolSyncPolicy.SecureUpgradeGuard().register(entry) == SchoolSyncPolicy.UpgradeResult.ALLOW, "新浏览器窗口具有独立状态");
        SchoolSyncPolicy.SecureUpgradeGuard duplicates = new SchoolSyncPolicy.SecureUpgradeGuard();
        require(duplicates.register(callback) == SchoolSyncPolicy.UpgradeResult.ALLOW, "HTTPS回调排队");
        for (int i = 0; i < SchoolSyncPolicy.SecureUpgradeGuard.MAX_DUPLICATES; i++) {
            require(duplicates.register(callback) == SchoolSyncPolicy.UpgradeResult.DUPLICATE, "有限重复由浏览器取消本次请求，不能打断已排队的HTTPS回调");
        }
        require(duplicates.getAttemptCount() == 1, "合并重复不能触发票据重放");
        require(duplicates.register(callback) == SchoolSyncPolicy.UpgradeResult.LIMIT, "重复请求也有总次数限制");
        String cas = "https://c.nua.edu.cn/cas/login?service=http%3A%2F%2Fjwnew.nua.edu.cn%2Fsso.jsp";
        require(SchoolSyncPolicy.isAcademicDownload("https://jwnew.nua.edu.cn/jsxsd/observed-export?term=1"), "只接受实际教务HTTPS文件请求");
        require(!SchoolSyncPolicy.isAcademicDownload("http://jwnew.nua.edu.cn/export.xls"), "不发送明文下载");
        require(!SchoolSyncPolicy.isAcademicDownload("https://jwnew.nua.edu.cn.attacker.example/export.xls"), "不向相似域名发送Cookie");
        require(!SchoolSyncPolicy.isAcademicDownload("https://c.nua.edu.cn/cas/login"), "CAS登录不是下载地址");
        require(!SchoolSyncPolicy.isAcademicDownload("https://jwnew.nua.edu.cn/sso.jsp?ticket=ST-secret"), "不在下载流程重放SSO票据");
        require(!SchoolSyncPolicy.isAcademicDownload("https://jwnew.nua.edu.cn/export.xls?ticket=ST-secret"), "下载查询票据拒绝");
        require(!SchoolSyncPolicy.isAcademicDownload("https://jwnew.nua.edu.cn/export.xls?%74icket=ST-secret"), "编码票据参数也不能重放");
        require(!SchoolSyncPolicy.isAcademicDownload("https://user@jwnew.nua.edu.cn/export.xls"), "禁止URL内嵌凭据");
        require(!SchoolSyncPolicy.isAcademicDownload("https://jwnew.nua.edu.cn:8443/export.xls"), "不扩大到未知端口");
        String vpnRoute = "/https/0123456789abcdef0123456789abcdefabcdef0123456789";
        String vpnHome = "https://v.nua.edu.cn" + vpnRoute + "/jsxsd/framework/xsMain.jsp";
        String vpnExport = "https://v.nua.edu.cn" + vpnRoute + "/jsxsd/xskb/xskb_print.do?term=2026-2027-1";
        require(SchoolSyncPolicy.isAcademicDownload(vpnExport, vpnHome), "用户日志中的外网网关导出，在当前教务路由内允许");
        require(SchoolSyncPolicy.isAcademicDownload(vpnExport.replace("v.nua.edu.cn", "V.NUA.EDU.CN:443"), vpnHome), "网关标准端口及域名大小写");
        require(!SchoolSyncPolicy.isAcademicDownload(vpnExport), "不能单凭网关域名允许下载");
        require(!SchoolSyncPolicy.isAcademicDownload(vpnExport, "https://v.nua.edu.cn/"), "网关登录首页不是教务来源");
        require(!SchoolSyncPolicy.isAcademicDownload(vpnExport, "https://jwnew.nua.edu.cn/jsxsd/framework/xsMain.jsp"), "不将校园网会话复用给外网路由");
        require(SchoolSyncPolicy.academicDownloadReferer(vpnExport, vpnHome + "?secret=test#private").equals(vpnHome), "网关Referer保留实际路由但剔除查询及片段");
        require(SchoolSyncPolicy.displayAddress(vpnExport).equals("https://v.nua.edu.cn/https/*/jsxsd/xskb/xskb_print.do"), "诊断仍隐藏网关路由及下载参数");
        for (String invalid : new String[]{
                vpnExport.replace("012345", "fedcba"),
                vpnExport.replace("https://", "http://"),
                vpnExport.replace("v.nua.edu.cn", "v.nua.edu.cn.attacker.example"),
                vpnExport.replace("v.nua.edu.cn", "user@v.nua.edu.cn"),
                vpnExport.replace("v.nua.edu.cn", "v.nua.edu.cn:8443"),
                vpnExport.replace(vpnRoute, "/https/*"),
                vpnExport.replace("/https/", "/http/"),
                vpnExport.replace("/jsxsd/xskb/", "/jsxsd/grsz/"),
                vpnExport.replace("/jsxsd/xskb/", "/jsxsd/xskb/../grsz/"),
                vpnExport.replace("/jsxsd/xskb/", "/jsxsd/xskb/%2e%2e/grsz/"),
                vpnExport.replace("/jsxsd/xskb/", "/jsxsd%2fxskb/"),
                vpnExport.replace("/jsxsd/xskb/", "/jsxsd//xskb/"),
                vpnExport + "&%74icket=ST-test-only",
                vpnExport + "&password=test-only"}) {
            require(!SchoolSyncPolicy.isAcademicDownload(invalid, vpnHome), "网关拒绝跨路由、凭据、明文或非课表目标");
        }
        require(!SchoolSyncPolicy.isAcademicDownload(vpnExport, vpnHome.replace("/framework/xsMain.jsp", "/grsz/grsz_xgrxx.do")), "个人设置页不能作为网关取课来源");
        String portalUrl = "https://i.nua.edu.cn/";
        String homeUrl = "https://jwnew.nua.edu.cn/jsxsd/framework/xsMain.jsp";
        String profileUrl = "https://jwnew.nua.edu.cn/jsxsd/grsz/grsz_xgrxx.do";
        require(SchoolSyncPolicy.isPersonalSettings(profileUrl), "截图中的个人信息页须独立识别");
        require(!SchoolSyncPolicy.isPersonalSettings(homeUrl), "教务首页不是个人信息页");
        require(SchoolSyncPolicy.canAutoOpenFrame(portalUrl, homeUrl), "门户可进入实际嵌入的已确认教务首页");
        require(!SchoolSyncPolicy.canAutoOpenFrame(homeUrl, profileUrl), "教务主页不能提升个人中心框架");
        require(!SchoolSyncPolicy.canAutoOpenFrame(portalUrl, profileUrl), "门户不能自动进入资料修改页");
        require(!SchoolSyncPolicy.canAutoOpenFrame(homeUrl, homeUrl), "教务内不重复提升自己的框架");
        require(!SchoolSyncPolicy.canAutoOpenFrame(portalUrl, homeUrl + "?%74icket=ST-secret"), "框架票据编码也不能重放");
        require(!SchoolSyncPolicy.canAutoOpenFrame(portalUrl, "https://jwnew.nua.edu.cn:8443/jsxsd/framework/xsMain.jsp"), "框架提升限制端口");
        require(SchoolSyncPolicy.trusted(cas) && !SchoolSyncPolicy.canRead(cas), "CAS地址的嵌套HTTP service必须保留，认证本身走HTTPS且不注入脚本");
        require(SchoolSyncPolicy.displayAddress(entry).equals(SchoolSyncPolicy.displayAddress(callback)), "诊断同路径去参，但防循环必须区分两者");
        require(SchoolSyncPolicy.displayAddress("http://jwnew.nua.edu.cn/jsxsd/?ticket=secret").equals("http://jwnew.nua.edu.cn/jsxsd/"), "被阻止的HTTP目标必须可诊断但不含票据");
        require(SchoolSyncPolicy.displayAddress("https://outside.example/path?password=secret").equals("https://outside.example/path"), "未知域名也保留协议和域名");
        require(SchoolSyncPolicy.displayAddress("javascript:passwordSecret").equals("javascript:（省略地址正文）"), "不记录非网页协议正文");
        String safe = SchoolSyncPolicy.displayAddress("https://i.nua.edu.cn/sopcb/?ticket=ST-secret#token");
        require(!safe.contains("secret") && !safe.contains("token"), "诊断不能包含查询参数或登录票据");
        require(!SchoolSyncPolicy.displayAddress("https://jwnew.nua.edu.cn/student/263601064/").contains("263601064"), "路径学号脱敏");
        System.out.println("School sync policy validation passed: SSO entry/ticket callback distinction, bounded HTTPS upgrades, protected CAS, safe diagnostics.");
    }
}
