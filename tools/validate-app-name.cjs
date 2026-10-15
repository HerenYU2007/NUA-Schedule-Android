const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8');
const strings = read('app/src/main/res/values/strings.xml');
assert(strings.includes('<string name="app_name">南艺电子课表</string>'));
assert(strings.includes('<string name="widget_label">南艺电子课表 · 今日列表（深色）</string>'));
assert(strings.includes('<string name="white_widget_label">南艺电子课表 · 今日列表（白色）</string>'));
assert(strings.includes('<string name="widget_description">南艺电子课表：'));
assert(read('app/src/main/AndroidManifest.xml').includes('android:label="@string/app_name"'));
assert(read('app/src/main/java/cn/nua/wenguan/schedule/MainActivity.java').includes('getString(R.string.app_name)'), 'Name resource used in the mine page of the three-tab home');
for (const file of ['ScheduleSyncActivity.java', 'SettingsActivity.java']) {
  const source = read('app/src/main/java/cn/nua/wenguan/schedule/' + file);
  assert(source.includes('getString(R.string.app_name)'), 'Name resource used by ' + file);
}
function check(directory) {
  for (const entry of fs.readdirSync(directory, {withFileTypes: true})) {
    const full = path.join(directory, entry.name);
    if (entry.isDirectory()) check(full);
    else if (/\.(java|xml|js)$/.test(entry.name)) {
      assert(!fs.readFileSync(full, 'utf8').includes('南艺文管课表'), 'Old display name remains: ' + entry.name);
    }
  }
}
check(path.join(root, 'app/src/main'));
assert(read('app/build.gradle.kts').includes('applicationId = "cn.nua.wenguan.schedule"'), 'Package unchanged');
console.log('Application naming validation passed: 南艺电子课表 launcher, header, widget, help and diagnostics; package unchanged, no old display-name literals.');
