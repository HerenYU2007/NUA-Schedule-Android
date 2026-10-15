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
const clock = java('data/SchoolPeriodTimes.java');
for (const name of ['start','end']) {
  assert(placement.includes('SchoolPeriodTimes.'+name+'(')&&parser.includes('SchoolPeriodTimes.'+name+'('),
    'Grid and import must use one shared school clock: '+name);
}
const times = key => [...clock.match(new RegExp('String\\[\\] '+key+'\\s*=\\s*\\{([^}]+)\\}'))[1].matchAll(/"([^"]*)"/g)].map(m=>m[1]);
assert.deepEqual(times('START'),['','08:00','08:55','10:00','10:55','14:00','14:55','16:00','16:55','18:30','19:25','20:30','21:25']);
assert.deepEqual(times('END'),['','08:45','09:40','10:45','11:40','14:45','15:40','16:45','17:40','19:15','20:10','21:15','22:00']);
console.log('Appearance/manual-import guards passed: theme choices, persistence, screen contexts, schedule isolation and school times.');
