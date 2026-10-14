'use strict';

// GitHub counters only; no APK downloads, application analytics or third-party chart service.
const fs = require('node:fs');
const path = require('node:path');
const REPOSITORY = 'HerenYU2007/NUA-Schedule-Android';
const TIMEZONE = 'Asia/Shanghai';
const START = '<!-- APK-DOWNLOAD-STATS:START -->';
const END = '<!-- APK-DOWNLOAD-STATS:END -->';
const DAY_MS = 86400000;

function integer(value, label) {
  if (!Number.isSafeInteger(value) || value < 0) throw new Error(`Invalid ${label}`);
  return value;
}
function dateKey(instant) {
  const date = new Date(instant);
  if (!Number.isFinite(date.getTime())) throw new Error('Invalid sample date');
  const fields = new Intl.DateTimeFormat('en-US', { timeZone: TIMEZONE, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(date);
  const part = type => fields.find(field => field.type === type).value;
  return `${part('year')}-${part('month')}-${part('day')}`;
}
function dayNumber(date) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) throw new Error('Invalid calendar day');
  const number = Date.parse(date + 'T00:00:00Z') / DAY_MS;
  if (!Number.isFinite(number) || new Date(number * DAY_MS).toISOString().slice(0, 10) !== date) throw new Error('Invalid calendar day');
  return number;
}
function blankState() {
  return { schemaVersion: 1, repository: REPOSITORY, timezone: TIMEZONE, startedAt: '', updatedAt: '', assets: [], daily: [] };
}
function validateState(state) {
  if (!state || state.schemaVersion !== 1 || state.repository !== REPOSITORY || state.timezone !== TIMEZONE || !Array.isArray(state.assets) || !Array.isArray(state.daily)) throw new Error('Statistics history is invalid; refusing to reset it');
  const ids = new Set();
  for (const asset of state.assets) {
    integer(asset.id, 'asset ID'); integer(asset.count, 'asset counter');
    if (!asset.id || ids.has(asset.id)) throw new Error('Duplicate asset in history');
    ids.add(asset.id);
  }
  let previousDate = '', previousTotal = -1;
  for (const entry of state.daily) {
    dayNumber(entry.date); integer(entry.total, 'daily total'); integer(entry.currentAssets, 'asset count'); integer(entry.currentTotal, 'current asset total');
    if (entry.date <= previousDate || entry.total < previousTotal || !Number.isFinite(Date.parse(entry.sampledAt))) throw new Error('Invalid or non-monotonic daily history');
    previousDate = entry.date; previousTotal = entry.total;
  }
  if (state.daily.length && (!Number.isFinite(Date.parse(state.startedAt)) || !Number.isFinite(Date.parse(state.updatedAt)))) throw new Error('Invalid history timestamps');
  return state;
}
function apkAssets(assets) {
  if (!Array.isArray(assets)) throw new Error('Invalid release asset response');
  return assets.filter(asset => {
    if (!asset || typeof asset.name !== 'string' || typeof asset.state !== 'string') throw new Error('Invalid release asset');
    return asset.state === 'uploaded' && /\.apk$/i.test(asset.name);
  }).map(asset => ({ id: integer(asset.id, 'asset ID'), count: integer(asset.download_count, 'download counter') }));
}
async function paginated(endpoint, getJson) {
  const result = [];
  for (let page = 1; page <= 1000; page++) {
    const items = await getJson(`${endpoint}?per_page=100&page=${page}`);
    if (!Array.isArray(items)) throw new Error('Invalid paginated GitHub response');
    result.push(...items);
    if (items.length < 100) return result;
  }
  throw new Error('Pagination limit reached; refusing a partial sample');
}
async function collect(getJson) {
  const base = `https://api.github.com/repos/${REPOSITORY}`;
  const releases = await paginated(`${base}/releases`, getJson);
  const byId = new Map();
  for (const release of releases) {
    if (!release || typeof release.draft !== 'boolean') throw new Error('Invalid release response');
    if (release.draft) continue;
    integer(release.id, 'release ID');
    // Fetch assets separately: never assume the embedded list includes all attachments.
    const assets = apkAssets(await paginated(`${base}/releases/${release.id}/assets`, getJson));
    for (const asset of assets) {
      if (!asset.id) throw new Error('Invalid asset ID');
      const old = byId.get(asset.id);
      if (old && old.count !== asset.count) throw new Error('Inconsistent asset counter during pagination');
      byId.set(asset.id, asset);
    }
  }
  return [...byId.values()].sort((a, b) => a.id - b.id);
}
async function getJson(target) {
  const allowed = `https://api.github.com/repos/${REPOSITORY}/`;
  if (!target.startsWith(allowed)) throw new Error('Untrusted statistics URL');
  const headers = { Accept: 'application/vnd.github+json', 'X-GitHub-Api-Version': '2022-11-28', 'User-Agent': 'NUA-Schedule-APK-Download-Stats' };
  if (process.env.GITHUB_TOKEN) headers.Authorization = `Bearer ${process.env.GITHUB_TOKEN}`;
  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      const response = await fetch(target, { headers, redirect: 'error', signal: AbortSignal.timeout(15000) });
      if (!response.ok) throw new Error(`GitHub statistics HTTP ${response.status}`);
      const text = await response.text();
      if (text.length > 8 * 1024 * 1024) throw new Error('GitHub response exceeds the statistics limit');
      return JSON.parse(text);
    } catch (error) {
      if (attempt) throw new Error(error.message.startsWith('GitHub statistics HTTP ') ? error.message : 'GitHub statistics request failed');
    }
  }
}
function mergeSample(previous, assets, instant = new Date().toISOString()) {
  validateState(previous);
  const timestamp = new Date(instant).toISOString(), date = dateKey(timestamp);
  if (previous.updatedAt && timestamp < previous.updatedAt) throw new Error('Out-of-order sample; preserving newer statistics');
  const byId = new Map(previous.assets.map(asset => [asset.id, asset.count]));
  const seen = new Set(); let currentTotal = 0;
  for (const asset of assets) {
    integer(asset.id, 'asset ID'); integer(asset.count, 'download counter');
    if (!asset.id || seen.has(asset.id)) throw new Error('Duplicate or invalid APK asset');
    seen.add(asset.id); currentTotal += asset.count;
    // Retain previously observed downloads if an APK is removed/replaced, or API data briefly regresses.
    byId.set(asset.id, Math.max(byId.get(asset.id) ?? 0, asset.count));
  }
  const registry = [...byId].map(([id, count]) => ({ id, count })).sort((a, b) => a.id - b.id);
  const total = registry.reduce((sum, asset) => sum + asset.count, 0);
  integer(total, 'total counter'); integer(currentTotal, 'current total');
  const last = previous.daily.at(-1);
  const changed = !last || last.date !== date || last.total !== total || last.currentTotal !== currentTotal || last.currentAssets !== assets.length || JSON.stringify(previous.assets) !== JSON.stringify(registry);
  if (!changed) return { changed: false, state: previous };
  if (last && date < last.date) throw new Error('Out-of-order day');
  const daily = previous.daily.map(item => ({ ...item }));
  const point = { date, total, currentTotal, currentAssets: assets.length, sampledAt: timestamp };
  if (last?.date === date) daily[daily.length - 1] = point; else daily.push(point);
  const state = { schemaVersion: 1, repository: REPOSITORY, timezone: TIMEZONE, startedAt: previous.startedAt || timestamp, updatedAt: timestamp, assets: registry, daily };
  return { changed: true, state: validateState(state) };
}
function escapeXml(value) { return String(value).replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&apos;' }[char])); }
function clockLabel(timestamp) {
  return new Intl.DateTimeFormat('zh-CN', { timeZone: TIMEZONE, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date(timestamp));
}
function renderSvg(state) {
  validateState(state);
  const latest = state.daily.at(-1), total = latest?.total ?? 0;
  const recent = latest ? state.daily.filter(point => dayNumber(point.date) >= dayNumber(latest.date) - 29) : [];
  const width = 980, height = 490, left = 84, right = 932, top = 192, bottom = 373;
  const firstDay = recent.length ? dayNumber(recent[0].date) : 0, finalDay = recent.length ? dayNumber(recent.at(-1).date) : 0;
  const scale = Math.max(1, total / 5), power = 10 ** Math.floor(Math.log10(scale));
  const step = [1, 2, 5, 10].find(value => value * power >= scale) * power, maxY = step * 5;
  const x = point => recent.length === 1 ? (left + right) / 2 : left + (dayNumber(point.date) - firstDay) / Math.max(1, finalDay - firstDay) * (right - left);
  const y = point => bottom - point.total / maxY * (bottom - top);
  const fmt = value => new Intl.NumberFormat('en-US').format(value);
  let graph = '';
  for (let tick = 0; tick <= 5; tick++) {
    const yy = bottom - tick / 5 * (bottom - top);
    graph += `<line class="grid" x1="${left}" y1="${yy}" x2="${right}" y2="${yy}"/><text class="muted" x="${left - 12}" y="${yy + 5}" text-anchor="end" font-size="13">${fmt(tick * step)}</text>`;
  }
  const line = recent.map((point, index) => `${index && dayNumber(point.date) - dayNumber(recent[index - 1].date) === 1 ? 'L' : 'M'}${x(point).toFixed(2)},${y(point).toFixed(2)}`).join(' ');
  graph += `<path class="line" d="${line}" fill="none" stroke-width="3" stroke-linejoin="round" stroke-linecap="round"/>`;
  const labelIndices = new Set(recent.length < 7 ? recent.map((_, index) => index) : [0, ...Array.from({ length: 5 }, (_, index) => Math.round((index + 1) * (recent.length - 1) / 6)), recent.length - 1]);
  recent.forEach((point, index) => {
    graph += `<circle class="point" cx="${x(point).toFixed(2)}" cy="${y(point).toFixed(2)}" r="${recent.length > 15 ? 3 : 4.5}"><title>${escapeXml(point.date)}：${point.total} 次</title></circle>`;
    if (labelIndices.has(index)) graph += `<text class="muted" x="${x(point).toFixed(2)}" y="${bottom + 26}" text-anchor="middle" font-size="13">${escapeXml(point.date.slice(5).replace('-', '/'))}</text>`;
  });
  const status = latest ? `最近数据变动：${clockLabel(state.updatedAt)}（北京时间）` : '尚未开始采样，等待统计工作流首次运行';
  const note = recent.length === 1 ? '已记录 1 天；从首次采样开始积累每日折线，不回填虚构历史。' : '最近30天 · 每天一个累计值 · 今日数据持续更新 · 缺失日期不补造数据';
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-labelledby="title desc">
<title id="title">全部版本 APK 累计下载量：${fmt(total)} 次</title>
<desc id="desc">北京时间按日记录累计总下载量。只包含已发布Release中的APK，包含预发布，不包含源码ZIP、HAP或更新清单。${escapeXml(status)}</desc>
<style>text{font-family:system-ui,-apple-system,"Segoe UI","Microsoft YaHei",sans-serif;fill:#243746}.card{fill:#f7f9fb;stroke:#c6d5df}.muted{fill:#5b6b78}.grid{stroke:#e0e8ee;stroke-width:1}.line{stroke:#1f4e78}.point{fill:#1f4e78;stroke:#fff;stroke-width:2}.pill{fill:#e3eef7}@media(prefers-color-scheme:dark){text{fill:#e8eff5}.card{fill:#121b24;stroke:#3d5263}.muted{fill:#bac8d4}.grid{stroke:#2d4050}.line{stroke:#9ed6ff}.point{fill:#9ed6ff;stroke:#121b24}.pill{fill:#223849}}</style>
<rect class="card" x="1" y="1" width="978" height="488" rx="22"/>
<text x="40" y="46" font-size="21" font-weight="700">APK 累计下载量</text>
<text x="40" y="102" font-size="43" font-weight="750">${fmt(total)}<tspan class="muted" font-size="18" font-weight="400"> 次</tspan></text>
<rect class="pill" x="700" y="27" width="230" height="35" rx="17"/><text x="815" y="50" text-anchor="middle" font-size="13">全部版本合并 · 按日累计</text>
<text class="muted" x="40" y="133" font-size="14">${escapeXml(status)}</text>
<text class="muted" x="40" y="162" font-size="13">约每5分钟检查，仅数值或日期变化时更新；纵轴为累计次数，不是每天新增。</text>
${graph}
<text class="muted" x="40" y="440" font-size="13">${escapeXml(note)}</text>
<text class="muted" x="40" y="467" font-size="12">来源：GitHub Release 附件计数 · 下载次数≠安装人数 · 镜像缓存下载可能未计入</text>
</svg>\n`;
}
function readmeBlock(state) {
  const stamp = state.updatedAt ? Date.parse(state.updatedAt) : 'pending';
  return `${START}\n## APK 下载趋势\n\n![所有版本 APK 按日累计下载量](docs/downloads/apk-downloads.svg?v=${stamp})\n\n所有版本合并，按北京时间每天记录一个**累计总下载量**；约每5分钟检查，数值或日期变化时更新。只统计 APK，不计 ZIP、HAP 和更新清单。历史从首次采样开始记录，镜像缓存下载可能不完整。\n\n[查看统计说明与历史数据](docs/downloads/README.md) · [查看最近检查记录](https://github.com/${REPOSITORY}/actions/workflows/apk-download-stats.yml)\n${END}`;
}
function updateReadme(readme, state) {
  const begin = readme.indexOf(START), end = readme.indexOf(END);
  if (begin < 0 || end < begin || readme.indexOf(START, begin + START.length) >= 0 || readme.indexOf(END, end + END.length) >= 0) throw new Error('README statistics markers missing/duplicated; refusing broad edits');
  return readme.slice(0, begin) + readmeBlock(state) + readme.slice(end + END.length);
}
function atomicWrite(file, contents) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  const temp = file + '.tmp';
  try { fs.writeFileSync(temp, contents, 'utf8'); fs.renameSync(temp, file); }
  finally { if (fs.existsSync(temp)) fs.unlinkSync(temp); }
}
async function main() {
  const root = path.resolve(__dirname, '..'), history = path.join(root, 'docs/downloads/history.json');
  const previous = fs.existsSync(history) ? validateState(JSON.parse(fs.readFileSync(history, 'utf8'))) : blankState();
  // Complete every API read before modifying any tracked output. Failures preserve prior graph/history.
  const result = mergeSample(previous, await collect(getJson));
  const readmePath = path.join(root, 'README.md'), svgPath = path.join(root, 'docs/downloads/apk-downloads.svg');
  const readme = fs.readFileSync(readmePath, 'utf8'), nextReadme = updateReadme(readme, result.state);
  const svg = renderSvg(result.state);
  if (result.changed || !fs.existsSync(svgPath) || readme !== nextReadme) {
    atomicWrite(history, JSON.stringify(result.state, null, 2) + '\n');
    atomicWrite(svgPath, svg); atomicWrite(readmePath, nextReadme);
  }
  const latest = result.state.daily.at(-1);
  console.log(`APK statistics: total=${latest.total}, daily_points=${result.state.daily.length}, changed=${result.changed}`);
}
module.exports = { REPOSITORY, TIMEZONE, blankState, dateKey, dayNumber, validateState, apkAssets, collect, mergeSample, renderSvg, updateReadme, readmeBlock };
if (require.main === module) main().catch(error => { console.error(error.message); process.exitCode = 1; });
