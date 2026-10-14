// Static lifecycle/UX guards; this does not replace Android device UI testing.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const source = fs.readFileSync(path.join(root,
  'app/src/main/java/cn/nua/wenguan/schedule/ScheduleSyncActivity.java'), 'utf8');
const strings = fs.readFileSync(path.join(root, 'app/src/main/res/values/strings.xml'), 'utf8');
function method(name) {
  const match = source.match(new RegExp('(?:private|protected) (?:void|boolean) ' + name + '\\([^)]*\\) \\{'));
  assert(match, name + ' exists');
  let depth = 1, end = match.index + match[0].length;
  const start = end;
  while (depth && end < source.length) {
    if (source[end] === '{') depth++;
    if (source[end] === '}') depth--;
    end++;
  }
  return source.slice(start, end - 1);
}
assert(strings.includes('正在获取课表，请稍后'));
assert(strings.includes('隐藏提示（继续获取）'));
const show = method('setFetchingNotice');
assert(show.includes('!active || reviewing || flowStopped || isFinishing() || isDestroyed()'));
assert(show.includes('fetchingNoticeHidden || fetchingDialog != null'));
assert(show.includes('new ProgressBar(this)'));
assert(show.includes('setCanceledOnTouchOutside(false)'));
assert(show.includes('fetchingNoticeHidden = true'));
assert(!/cancelFileRead|stopLoading|removeCallbacks|restartFlow/.test(show), 'Hide never cancels fetching');
assert(method('prepareRetry').includes('fetchingNoticeHidden = false'));
assert(method('navigate').includes('setFetchingNotice(!"login".equals(kind) && !"sso_required".equals(kind))'));
for (const name of ['showBrowserProblem', 'showPreview', 'cancelFileRead']) {
  assert(method(name).includes('setFetchingNotice(false)'), name + ' hides notice');
}
assert(method('beginFileRead').includes('setFetchingNotice(true)'));
for (const name of ['onPause', 'onDestroy']) {
  assert(method(name).includes('dismissFetchingDialog()'), name + ' releases window');
}
assert(method('onResume').includes('setFetchingNotice(fetchingNoticeActive)'));
console.log('Fetching notice static guards passed: text, spinner, authentication, hide without cancellation, preview/error, retry and lifecycle. Device UI not exercised.');
