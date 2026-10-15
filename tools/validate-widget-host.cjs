const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8');
const gradle = read('app/build.gradle.kts');
const pkg = gradle.match(/namespace\s*=\s*"([^"]+)"/)[1];
const javaRoot = 'app/src/main/java/' + pkg.replaceAll('.', '/');
const provider = read(javaRoot + '/widget/TodayScheduleWidgetProvider.java');
const main = read(javaRoot + '/MainActivity.java');
const settings = read(javaRoot + '/SettingsActivity.java');
const layout = read('app/src/main/res/layout/widget_today.xml');
const phoneLayout = read('app/src/main/res/layout/widget_phone.xml');
const phoneStyles = read('app/src/main/res/values/widget_phone_styles.xml');
const info = read('app/src/main/res/xml/today_schedule_widget_info.xml');
const manifest = read('app/src/main/AndroidManifest.xml');
const allowedViews = new Set(['LinearLayout', 'TextView']);
for (const [, tag] of (layout + phoneLayout).matchAll(/<([A-Za-z][\w.]*)\b/g)) {
  assert(allowedViews.has(tag), 'Only RemoteViews-supported views: ' + tag);
}
for (const [, id] of provider.matchAll(/R\.id\.(\w+)/g)) {
  assert((layout + phoneLayout).includes('@+id/' + id), 'Missing widget view ' + id);
}
assert(provider.includes('onAppWidgetOptionsChanged'), 'Re-render after resize');
assert(provider.includes('Build.VERSION.SDK_INT >= 31') && provider.includes('OPTION_APPWIDGET_SIZES'), 'Exact Android 12+ host sizes');
assert(provider.includes('new RemoteViews(exactViews)'), 'Independent orientation/size views');
assert(provider.includes('new RemoteViews(buildViews(context, maxWidth, minHeight), buildViews(context, minWidth, maxHeight))'), 'Fallback for hosts without exact sizes');
assert(provider.includes('exactViews.size() >= 16'), 'Bound remote view count');
assert(provider.includes('WidgetSizePolicy.current') && provider.includes('WidgetSizePolicy.range'), 'Validate stale exact sizes and normalize invalid legacy options');
assert(provider.includes('i < rowLimit'), 'Do not clip three fixed course rows');
assert(provider.includes('visibleSessions.size() - rowLimit') && layout.includes('@+id/widget_more'), 'Hidden courses must remain discoverable');
assert(provider.includes('smallestScreenWidthDp >= 600'), 'Tablet detection independent of widget height/orientation');
assert(provider.includes('layout.phoneLayout ? R.layout.widget_phone : R.layout.widget_today')&&provider.includes('layout.phoneLayout ? R.layout.widget_phone_white : R.layout.widget_today_white'), 'Separate phone layout and independent white variants');
const courseTag = phoneLayout.match(/<LinearLayout\b[^>]*android:id="@\+id\/widget_courses"[^>]*>/s)?.[0];
assert(courseTag && courseTag.includes('android:gravity="top"'), 'Course list must start at top, not vertically centred');
assert(!courseTag.includes('center_vertical'), 'No vertical centring of the whole course list');
assert(phoneLayout.indexOf('widget_course_row_1') < phoneLayout.indexOf('widget_course_row_2')
  && phoneLayout.indexOf('widget_course_row_2') < phoneLayout.indexOf('widget_course_row_3'), 'Courses ordered downwards');
assert(phoneStyles.includes('name="android:gravity">center'), 'Course status remains centred at right');
assert(phoneStyles.includes('name="PhoneWidgetCourseName"') && phoneStyles.includes('name="android:layout_width">0dp'), 'Restore weighted inline phone course name');
assert((main + settings).includes('isRequestPinAppWidgetSupported') && (main + settings).includes('requestPinAppWidget(component, null, null)'), 'Supported launcher pin request');
assert((main + settings).includes('catch (RuntimeException ignored)'), 'Unsupported OEM pinning must not crash');
assert(manifest.includes('MY_PACKAGE_REPLACED') && manifest.includes('TIME_SET'), 'Upgrade and clock updates');
assert(info.includes('targetCellWidth="4"') && info.includes('targetCellHeight="2"'), 'Keep 4x2 default');
assert(info.includes('horizontal|vertical'), 'Both resize directions');
assert(info.includes('previewImage="@drawable/widget_picker_preview_bitmap"'), 'Bitmap preview');
const preview = fs.readFileSync(path.join(root, 'app/src/main/res/drawable-nodpi/widget_picker_preview_bitmap.png'));
assert.equal(preview.subarray(0, 8).toString('hex'), '89504e470d0a1a0a', 'Actual PNG preview');
console.log('Widget host validation passed: ' + pkg + ', exact sizes and legacy fallback, resize, compatible views, overflow, pinning, upgrade and bitmap preview.');
