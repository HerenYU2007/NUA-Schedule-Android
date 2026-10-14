const fs = require('node:fs');
const assert = require('node:assert/strict');
const path = require('node:path');
const root = path.resolve(__dirname, '../app/src/main');
const read = p => fs.readFileSync(path.join(root, p), 'utf8');
const java = p => read('java/cn/nua/wenguan/schedule/' + p);
const prefs = java('AppearanceSettings.java');
assert(prefs.includes('createConfigurationContext(config)'));
assert(prefs.includes('if (choice == 0) return c;'));
assert(prefs.includes('.commit()'));
for (const name of ['MainActivity', 'SettingsActivity', 'ScheduleSyncActivity']) assert(java(name + '.java').includes('extends AppearanceActivity'));
assert(read('res/values-night/styles.xml').includes('Theme.Material.NoActionBar'));
assert(read('res/values-night/colors.xml').includes('name="text_primary"'));
const main = java('MainActivity.java');
assert(main.includes('AppearanceSettings.saveGrid'));
assert(main.includes('WeekGrid.create'));
assert(main.includes('selected_date'));
const sync = java('ScheduleSyncActivity.java');
assert(sync.includes('if (!getIntent().getBooleanExtra("manual_import", false))'));
assert(sync.includes('setPositiveButton("手动导入 XLS"'));
assert(!prefs.includes('Cookie') && !prefs.includes('official_schedule'));
const parser = java('data/OfficialScheduleParser.java');
const placement = java('ui/PeriodGrid.java');
for (const key of ['START','END']) {
  const times = text => [...text.match(new RegExp('String\\[\\] ' + key + ' = \\{([^}]+)\\}'))[1].matchAll(/"([^"]*)"/g)].map(m => m[1]);
  assert.deepEqual(times(placement), times(parser), 'Grid and import must use the identical school clock times');
}
console.log('Appearance/manual-import guards passed: theme choices, persistence, screen contexts, schedule isolation and school times.');
