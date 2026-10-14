// Guards integration, not just the pure policy. No live account or network used.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const activity = fs.readFileSync(path.join(root,
  'app/src/main/java/cn/nua/wenguan/schedule/ScheduleSyncActivity.java'), 'utf8');
const listener = activity.slice(activity.indexOf('browser.setDownloadListener'), activity.indexOf('browser.setWebChromeClient'));
assert(listener.includes('downloadSourcePage(browser, url)'));
assert(listener.includes('SchoolSyncPolicy.isAcademicDownload(url, sourcePage)'));
assert(listener.includes('beginFileRead(null, url, browser.getSettings().getUserAgentString(), sourcePage)'));
assert(activity.includes('depth < 6; depth++, browser = openers.get(browser)'));
assert(activity.includes('CookieManager.getInstance().getCookie(url), sourcePage'));
const download = fs.readFileSync(path.join(root,
  'app/src/main/java/cn/nua/wenguan/schedule/data/AcademicExportDownload.java'), 'utf8');
assert(download.includes('setRequestProperty("Referer", referer)'));
assert(download.indexOf('academicDownloadReferer(url, sourcePage)') < download.indexOf('openConnection()'));
assert(!/setHostnameVerifier|setSSLSocketFactory|setDefaultHostnameVerifier|setDefaultSSLSocketFactory/.test(download));
console.log('WebVPN integration guards passed: source page/opener, scoped download, actual URL Cookie and route-aware Referer, default TLS checks.');
