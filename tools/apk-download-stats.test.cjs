'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const stats = require('./apk-download-stats.cjs');
const firstTime = '2026-10-14T15:00:00.000Z';
const asset = (id, count, name = `${id}.apk`) => ({ id, download_count: count, name, state: 'uploaded' });
const sample = (items, time = firstTime, previous = stats.blankState()) => stats.mergeSample(previous, items, time).state;

test('calendar uses Beijing day boundaries and rejects impossible dates', () => {
  assert.equal(stats.dateKey('2026-10-14T15:59:59Z'), '2026-10-14');
  assert.equal(stats.dateKey('2026-10-14T16:00:00Z'), '2026-10-15');
  assert.equal(stats.dayNumber('2024-02-29') + 1, stats.dayNumber('2024-03-01'));
  assert.throws(() => stats.dayNumber('2026-02-29'));
  assert.throws(() => stats.dayNumber('2026-13-01'));
  assert.throws(() => stats.dateKey('invalid'));
});

test('only uploaded APK filenames count, case insensitively', () => {
  const inputs = [asset(1, 5), asset(2, 6, 'app.APK'), asset(3, 90, 'source.zip'), asset(4, 80, 'harmony.hap'), asset(5, 70, 'update.json'), asset(6, 60, 'app.apk.sha256'), { ...asset(7, 50), state: 'starter' }];
  assert.deepEqual(stats.apkAssets(inputs), [{ id: 1, count: 5 }, { id: 2, count: 6 }]);
  assert.throws(() => stats.apkAssets([asset(1, -1)]));
  assert.throws(() => stats.apkAssets([asset(1, Number.MAX_SAFE_INTEGER + 1)]));
  assert.throws(() => stats.apkAssets([{}]));
});

test('collector paginates releases AND assets, includes prereleases, excludes drafts', async () => {
  const calls = [];
  const releases = Array.from({ length: 102 }, (_, index) => ({ id: index + 1, draft: index === 0, prerelease: index === 1 }));
  const allAssets = Array.from({ length: 101 }, (_, index) => asset(index + 1, index + 1));
  const result = await stats.collect(async target => {
    const url = new URL(target); calls.push(url.pathname + url.search);
    assert.equal(url.searchParams.get('per_page'), '100');
    const page = Number(url.searchParams.get('page'));
    const items = url.pathname.endsWith('/releases') ? releases : url.pathname.endsWith('/2/assets') ? allAssets : [];
    return items.slice((page - 1) * 100, page * 100);
  });
  assert.equal(result.length, 101);
  assert.equal(result.reduce((sum, item) => sum + item.count, 0), 5151);
  assert.ok(calls.some(target => target.endsWith('/releases?per_page=100&page=2')));
  assert.ok(calls.some(target => target.endsWith('/2/assets?per_page=100&page=2')));
  assert.ok(!calls.some(target => target.includes('/1/assets')));
});

test('collector deduplicates asset IDs and rejects inconsistent or partial responses', async () => {
  const releases = [{ id: 1, draft: false }, { id: 2, draft: false }];
  const request = async target => target.includes('/assets?') ? [asset(42, 8)] : releases;
  assert.deepEqual(await stats.collect(request), [{ id: 42, count: 8 }]);
  await assert.rejects(stats.collect(async target => target.includes('/assets?') ? [asset(42, target.includes('/1/assets') ? 8 : 9)] : releases));
  await assert.rejects(stats.collect(async target => {
    if (target.includes('/2/assets')) throw new Error('network');
    return request(target);
  }));
  await assert.rejects(stats.collect(async () => ({ message: 'error' })));
  await assert.rejects(stats.collect(async () => [{ id: 1 }]));
});

test('first sample is the baseline and same-day samples replace, not add', () => {
  const first = sample([{ id: 1, count: 10 }, { id: 2, count: 20 }]);
  assert.equal(first.daily.length, 1);
  assert.equal(first.daily[0].total, 30);
  assert.equal(first.daily[0].date, '2026-10-14');
  const sameDay = sample([{ id: 1, count: 11 }, { id: 2, count: 22 }], '2026-10-14T15:20:00Z', first);
  assert.equal(sameDay.daily.length, 1);
  assert.equal(sameDay.daily[0].total, 33);
  const nextDay = sample([{ id: 1, count: 11 }, { id: 2, count: 22 }], '2026-10-14T16:00:00Z', sameDay);
  assert.deepEqual(nextDay.daily.map(point => point.total), [33, 33]);
  const unchanged = stats.mergeSample(nextDay, nextDay.assets, '2026-10-14T16:05:00Z');
  assert.equal(unchanged.changed, false);
  assert.strictEqual(unchanged.state, nextDay);
  assert.throws(() => sample(nextDay.assets, firstTime, nextDay));
});

test('removed/replaced APKs and counter regressions never erase observed downloads', () => {
  const first = sample([{ id: 1, count: 10 }, { id: 2, count: 20 }]);
  const replaced = sample([{ id: 2, count: 18 }, { id: 3, count: 5 }], '2026-10-14T15:10:00Z', first);
  assert.equal(replaced.daily[0].total, 35);
  assert.equal(replaced.daily[0].currentTotal, 23);
  assert.equal(replaced.daily[0].currentAssets, 2);
  const removed = sample([], '2026-10-14T15:20:00Z', replaced);
  assert.equal(removed.daily[0].total, 35);
  assert.equal(removed.daily[0].currentTotal, 0);
  assert.equal(removed.assets.length, 3);
});

test('zero APKs is a valid observed baseline', () => {
  const state = sample([]);
  assert.equal(state.daily[0].total, 0);
  assert.match(stats.renderSvg(state), /累计下载量：0 次/);
});

test('corrupt history and unsafe samples fail closed without resetting history', () => {
  const original = sample([{ id: 1, count: 10 }]);
  const corruptions = [
    state => state.schemaVersion = 2,
    state => state.repository = 'other/repo',
    state => state.assets.push({ id: 1, count: 10 }),
    state => state.assets[0].count = -1,
    state => state.daily[0].date = '2026-02-30',
    state => state.daily[0].sampledAt = 'invalid',
    state => state.updatedAt = '',
    state => state.daily.push({ ...state.daily[0], date: '2026-10-15', total: 9 })
  ];
  for (const corrupt of corruptions) {
    const state = structuredClone(original); corrupt(state);
    assert.throws(() => stats.validateState(state));
  }
  assert.throws(() => sample([{ id: 1, count: 1 }, { id: 1, count: 2 }]));
  assert.throws(() => sample([{ id: 0, count: 1 }]));
  assert.throws(() => sample([{ id: 1, count: Number.MAX_SAFE_INTEGER }, { id: 2, count: 1 }]));
});

test('README replacement is limited to one explicitly marked statistics block', () => {
  const state = sample([{ id: 1, count: 12 }]);
  const start = '<!-- APK-DOWNLOAD-STATS:START -->', end = '<!-- APK-DOWNLOAD-STATS:END -->';
  const prefix = '# Existing release 1.2.2\n\n', suffix = '\nKeep historical release notes.\n';
  const output = stats.updateReadme(prefix + start + '\nold\n' + end + suffix, state);
  assert.ok(output.startsWith(prefix)); assert.ok(output.endsWith(suffix));
  assert.match(output, /apk-downloads\.svg\?v=\d+/);
  assert.equal(stats.updateReadme(output, state), output);
  for (const broken of ['', end + start, start + start + end, start + end + end]) assert.throws(() => stats.updateReadme(broken, state));
});

test('SVG shows cumulative daily values and breaks gaps rather than fabricating days', () => {
  const first = sample([{ id: 1, count: 1234 }]);
  assert.match(stats.renderSvg(first), /已记录 1 天/);
  const second = sample([{ id: 1, count: 1250 }], '2026-10-16T10:00:00Z', first);
  const svg = stats.renderSvg(second);
  assert.match(svg, /累计下载量：1,250 次/);
  assert.match(svg, /北京时间/);
  assert.match(svg, /不是每天新增/);
  assert.match(svg, /d="M[\d.,]+ M[\d.,]+"/);
  assert.doesNotMatch(svg, /undefined|NaN/);
  assert.equal(second.daily.length, 2);
});

test('SVG displays last 30 calendar days while JSON keeps full history', () => {
  let state = stats.blankState();
  for (let index = 0; index < 40; index++) state = sample([{ id: 1, count: index + 1 }], new Date(Date.UTC(2026, 8, 1 + index, 12)).toISOString(), state);
  assert.equal(state.daily.length, 40);
  const svg = stats.renderSvg(state);
  assert.equal((svg.match(/<circle /g) || []).length, 30);
  assert.doesNotMatch(svg, /2026-09-01：/);
  assert.match(svg, /2026-10-10：40/);
});

test('workflow limits generated commits to statistics files and does not publish releases', () => {
  const workflow = fs.readFileSync(path.join(__dirname, '../.github/workflows/apk-download-stats.yml'), 'utf8');
  assert.match(workflow, /contents: write/);
  assert.match(workflow, /2-57\/5 \* \* \* \*/);
  assert.match(workflow, /git add -- README\.md docs\/downloads\/history\.json docs\/downloads\/apk-downloads\.svg/);
  assert.doesNotMatch(workflow, /git add \.|force-push|git push --force|gh release create|upload-release-asset|gradlew/);
});
