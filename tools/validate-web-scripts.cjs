'use strict';
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const root = path.join(__dirname, '../app/src/main/assets');
const extract = fs.readFileSync(path.join(root, 'schedule-extract.js'), 'utf8');
const navigate = fs.readFileSync(path.join(root, 'schedule-navigation.js'), 'utf8');
function row(values) {
  return {children: values.map(v => ({tagName: 'TD', innerText: v})), closest() {return this.table;}};
}
function table(values) {
  const t = {rows: values.map(row)};
  for (const r of t.rows) r.table = t;
  return t;
}
function doc(host, tables = [], labels = []) {
  return {location: {protocol: 'https:', hostname: host, pathname: '/', href: 'https://' + host + '/'}, title: '学生个人课表', body: {innerText: '学生个人课表'},
    querySelector(selector) {return this.password ? {} : null;},
    querySelectorAll(selector) {
      if (selector === 'input[type=password]') return this.password ? [node('')] : [];
      if (selector === 'table') return tables;
      if (selector === 'input[placeholder]') return [];
      if (selector === 'select' || selector === 'iframe[src],frame[src]') return [];
      return labels;
    }};
}
function run(script, document, frames = []) { return vm.runInNewContext(script, {window: {document, frames}, URL}); }
function node(label, visible = true) {
  return {innerText: label, dataset: {}, getClientRects() {return visible ? [1] : [];},
    clicks: 0, click() {this.clicks++;}};
}
function nav(document, frames = []) { return JSON.parse(run(navigate, document, frames)); }
const headers = ['课程名称', '实际上课时间', '实际上课教室', '授课教师'];
const data = ['测试课程', '1-2周,周一,第1-2节', '文210\n校外课堂', '教师'];
const trusted = doc('jwnew.nua.edu.cn', [table([headers, data])]);
const result = JSON.parse(run(extract, trusted));
assert.equal(result.rows[0][2], '文210\n校外课堂', '教室换行必须保留');
assert.equal(result.rows.length, 1);
assert.equal(run(extract, doc('c.nua.edu.cn', [table([headers, data])])), '', 'CAS不能提取');
assert.equal(run(extract, doc('fake-nua.edu.cn', [table([headers, data])])), '', '相似域名不能提取');
trusted.password = true;
assert.equal(run(extract, trusted), '', '有密码输入框时不能提取');
trusted.password = false;
trusted.location.protocol = 'http:';
assert.equal(run(extract, trusted), '', '非HTTPS不能提取');
trusted.location.protocol = 'https:';
const split = doc('jwnew.nua.edu.cn', [table([headers]), table([data])]);
assert.equal(JSON.parse(run(extract, split)).rows.length, 1, '分离表头和内容');
const container = doc('i.nua.edu.cn');
assert.equal(JSON.parse(run(extract, container, [{document: doc('jwnew.nua.edu.cn', [table([headers, data])]), frames: []}])).rows.length, 1, '同源可访问frame中的表格');
trusted.body.innerText = '学生个人课表 共2页 下一页';
assert.equal(JSON.parse(run(extract, trusted)).warnings.length, 1, '分页警告');
const ragged = doc('jwnew.nua.edu.cn', [table([headers, data, ['短行课程', '未识别']])]);
assert.equal(JSON.parse(run(extract, ragged)).warnings.length, 1, '不完整行必须提示');
const elective = node('进入选课'), homepage = node('进入首页');
const entrance = doc('jwnew.nua.edu.cn', [], [elective, homepage]);
assert.equal(nav(entrance).action, '点击：进入首页');
assert.equal(elective.clicks, 0, '不能进入选课');
assert.equal(homepage.clicks, 1);
assert.equal(nav(entrance).kind, 'wait', '不重复点击同一入口');
const accordion = node('培养管理 ▼'), childMenu = node('个人课表 ›', false);
let childVisible = false;
childMenu.getClientRects = () => childVisible ? [1] : [];
accordion.click = () => { accordion.clicks++; childVisible = true; };
const treePage = doc('jwnew.nua.edu.cn', [], [accordion, childMenu]);
assert.equal(nav(treePage).action, '点击：培养管理', '带展开箭头的上级菜单仍可识别');
assert.equal(nav(treePage).action, '点击：个人课表', '展开后必须继续进入下级课表，不停在第一层');
assert.equal(accordion.clicks, 1, '不能反复开合上级菜单');
assert.equal(childMenu.clicks, 1);
assert.equal(nav(treePage).kind, 'wait', '正常轮询不重复点击下级菜单');
const afterRetry = JSON.parse(vm.runInNewContext(navigate, {window: {document: treePage, frames: [], __nuaSyncEpoch: 99}, URL}));
assert.equal(afterRetry.action, '点击：个人课表', '原地重试通过新轮次重置菜单去重，不需重建页面');
assert.equal(accordion.clicks, 1);
function nuaTermTree(expanded) {
  const cultivated = node('培养管理'), mine = node('我的课表 ▾'), term = node('• 学期课表');
  const courseQuery = node('课程课表查询'), adjustments = node('调停课查询');
  const otherQueries = node('各类课表查询'), genericQuery = node('课表查询'), enrollment = node('选课管理');
  let mineVisible = expanded, termVisible = expanded;
  mine.getClientRects = () => mineVisible ? [1] : [];
  term.getClientRects = () => termVisible ? [1] : [];
  cultivated.click = () => { cultivated.clicks++; mineVisible = !mineVisible; };
  mine.click = () => { mine.clicks++; termVisible = !termVisible; };
  const page = doc('jwnew.nua.edu.cn', [], [cultivated, mine, term, courseQuery, adjustments, otherQueries, genericQuery, enrollment]);
  page.title = '首页'; page.body.innerText = '首页';
  return {page, cultivated, mine, term, offBranch: [courseQuery, adjustments, otherQueries, genericQuery, enrollment]};
}
const collapsedNua = nuaTermTree(false);
assert.equal(nav(collapsedNua.page).action, '点击：培养管理', '截图确认的第一层菜单');
assert.equal(nav(collapsedNua.page).action, '点击：我的课表', '截图确认的第二层菜单');
assert.equal(nav(collapsedNua.page).action, '点击：学期课表', '必须进入截图中的第三层叶子菜单');
assert.equal(nav(collapsedNua.page).kind, 'wait', '等待课表框架加载时不再开合父菜单');
assert.equal(nav(collapsedNua.page).kind, 'wait');
assert.equal(collapsedNua.cultivated.clicks, 1);
assert.equal(collapsedNua.mine.clicks, 1);
assert.equal(collapsedNua.term.clicks, 1);
assert(collapsedNua.offBranch.every(n => n.clicks === 0), '不进入课程课表、调停课、各类课表或选课入口');
const expandedNua = nuaTermTree(true);
assert.equal(nav(expandedNua.page).action, '点击：学期课表', '已展开时优先叶子，不能把我的课表收起');
assert.equal(nav(expandedNua.page).kind, 'wait');
assert.equal(expandedNua.cultivated.clicks, 0);
assert.equal(expandedNua.mine.clicks, 0);
assert.equal(expandedNua.term.clicks, 1);
assert(expandedNua.offBranch.every(n => n.clicks === 0));
const homeList = node('列表'), homeQuery = node('查询');
const shellMenu = node('我的课表'), shellTerm = node('• 学期课表');
const academicShellPage = homePage([homeList, homeQuery, shellMenu, shellTerm]);
academicShellPage.title = '首页'; academicShellPage.body.innerText = '培养管理 我的课表 学期课表 通知';
assert.equal(nav(academicShellPage).action, '点击：学期课表', '首页侧栏文字不应触发通知列表按钮');
assert.equal(nav(academicShellPage).kind, 'wait', '叶子点击后等待框架，不触发首页无关查询');
assert.equal(homeList.clicks, 0);
assert.equal(homeQuery.clicks, 0);
const falseMenu = node('班级课表与个人课表');
assert.equal(nav(doc('jwnew.nua.edu.cn', [], [falseMenu])).kind, 'wait', '不能模糊匹配其他业务入口');
const titledMenu = node('图标按钮'); titledMenu.getAttribute = attr => attr === 'title' ? '个人课表' : null;
assert.equal(nav(doc('jwnew.nua.edu.cn', [], [titledMenu])).action, '点击：个人课表', '明确title标签也可以识别');
const casButton = node('进入首页');
assert.equal(nav(doc('c.nua.edu.cn', [], [casButton])).kind, 'login');
assert.equal(casButton.clicks, 0, 'CAS页面不能执行导航');
function homePage(labels = []) {
  const page = doc('jwnew.nua.edu.cn', [], labels);
  page.location.pathname = '/jsxsd/framework/xsMain.jsp';
  page.location.href = 'https://jwnew.nua.edu.cn' + page.location.pathname;
  return page;
}
const localLoginButton = node('进入首页');
const expiredHome = homePage([localLoginButton]); expiredHome.password = true;
expiredHome.querySelector = selector => selector === 'input[type=password]'
  ? {get value() { throw new Error('禁止读取学校密码'); }} : null;
assert.equal(nav(expiredHome).kind, 'sso_required', '相同首页URL返回密码表单时必须重做门户SSO');
assert.equal(nav(expiredHome).academicHome, false, '不能仅凭URL声称登录成功');
assert.equal(localLoginButton.clicks, 0, '不操作教务系统本地密码登录');
assert.equal(nav(homePage()).academicHome, false, '空首页地址也不能声称认证完成');
const personalMenu = node('个人课表');
const recognizedHome = nav(homePage([personalMenu]));
assert.equal(recognizedHome.academicHome, true, '首页必须有可见教务菜单才识别');
assert.equal(personalMenu.clicks, 1, '进入首页后继续自动点击个人课表');
assert.equal(nav(doc('i.nua.edu.cn'), [{document: expiredHome, frames: []}]).kind, 'sso_required', '教务框架返回密码页也须走门户SSO');
const hidden = node('进入首页', false);
assert.equal(nav(doc('jwnew.nua.edu.cn', [], [hidden])).kind, 'wait', '不点击隐藏元素');
const outer = node('进入首页'), inner = node('进入首页');
const duplicateLabels = doc('jwnew.nua.edu.cn', [], [outer, inner]);
nav(duplicateLabels); nav(duplicateLabels);
assert.equal(outer.clicks + inner.clicks, 1, '同一文字多个祖先不能重复打开窗口');
const vpn = doc('v.nua.edu.cn', [table([headers, data])], [node('进入首页')]);
vpn.location.pathname = '/https/school/cas/login';
assert.equal(nav(vpn).kind, 'login', 'VPN中的CAS登录也不能注入导航');
assert.equal(run(extract, vpn), '', 'VPN中的CAS不能读表');
const query = node('查询');
const emptyTable = doc('jwnew.nua.edu.cn', [table([headers])], [query]);
emptyTable.body.innerText = '学生个人课表 课程名称 实际上课时间';
assert.equal(nav(emptyTable).action, '获取当前学期课表', '只有表头时必须自动查询');
assert.equal(query.clicks, 1);
const readyPage = doc('jwnew.nua.edu.cn', [table([headers, data])], [node('进入首页')]);
readyPage.body.innerText = '学生个人课表 课程名称 实际上课时间 测试课程';
assert.equal(nav(readyPage).kind, 'ready', '有课表时不应离开去其他菜单');
const aliasHeaders = ['课程名', '上课时间', '上课地点', '教师'];
const aliasPage = doc('jwnew.nua.edu.cn', [table([aliasHeaders, data])]);
aliasPage.title = '学生课表查询'; aliasPage.body.innerText = '学生课表查询 课程名 上课时间';
assert.equal(JSON.parse(run(extract, aliasPage)).rows.length, 1, '提取脚本与解析器的课程名别名保持一致');
assert.equal(nav(aliasPage).kind, 'ready');
const termList = doc('jwnew.nua.edu.cn', [table([aliasHeaders, data])], [node('我的课表'), node('• 学期课表')]);
termList.title = '学期课表'; termList.body.innerText = '学期课表 课程名 上课时间';
assert.equal(nav(termList).kind, 'ready', '学期课表列表已经有数据时立即读取');
assert.equal(JSON.parse(run(extract, termList)).rows.length, 1, '学期课表标题支持课程列表提取');
const termFrameHome = nuaTermTree(true);
assert.equal(nav(termFrameHome.page, [{document: termList, frames: []}]).kind, 'ready', '实际课程在框架内时先读取，不反复点击父菜单');
const termQueryButton = node('查询');
const emptyTermList = doc('jwnew.nua.edu.cn', [table([headers])], [termQueryButton]);
emptyTermList.title = '学期课表'; emptyTermList.body.innerText = '学期课表 课程名称 上课时间';
assert.equal(nav(emptyTermList).action, '获取当前学期课表');
const listView = node('列表视图');
const gridPage = doc('jwnew.nua.edu.cn', [], [listView]);
assert.equal(nav(gridPage).action, '切换个人课表列表视图', '只点击实际页面提供的课表列表入口');
assert.equal(listView.clicks, 1);
assert.equal(nav(gridPage).kind, 'wait', '不反复切换列表视图');
const currentList = node('列表视图'), listQuery = node('查询');
const currentEmptyList = doc('jwnew.nua.edu.cn', [table([headers])], [currentList, listQuery]);
currentEmptyList.body.innerText = '个人课表 课程名称 上课时间';
assert.equal(nav(currentEmptyList).action, '获取当前学期课表', '已经有列表表头时查询，不能再切换同一列表造成刷新循环');
assert.equal(currentList.clicks, 0);
const nonTimetableList = node('列表');
const otherPage = doc('jwnew.nua.edu.cn', [], [nonTimetableList]);
otherPage.title = '教学通知'; otherPage.body.innerText = '教学通知';
assert.equal(nav(otherPage).kind, 'wait', '非个人课表页不能泛化点击列表按钮');
assert.equal(nonTimetableList.clicks, 0);
const blockedFrame = {get document() { throw new Error('Cross-origin'); }};
const structure = nav(readyPage, [blockedFrame]).structure;
assert.equal(structure.frameErrors, 1, '诊断只保留不可读框架数量');
assert.equal(structure.tables, 1);
assert.equal(structure.courseHeader, true);
assert(!JSON.stringify(structure).includes('测试课程'), '结构诊断不返回课程正文');
const breadcrumb = node('个人课表'), query2 = node('查询');
const emptyWithBreadcrumb = doc('jwnew.nua.edu.cn', [table([headers])], [breadcrumb, query2]);
emptyWithBreadcrumb.body.innerText = '个人课表 课程名称 上课时间';
assert.equal(nav(emptyWithBreadcrumb).action, '获取当前学期课表', '不能重新点当前课表面包屑导致循环');
assert.equal(breadcrumb.clicks, 0);
const framed = doc('i.nua.edu.cn');
const frameQuery = framed.querySelectorAll.bind(framed);
framed.querySelectorAll = selector => selector === 'iframe[src],frame[src]'
  ? [{getAttribute() { return 'https://jwnew.nua.edu.cn/jsxsd/framework/xsMain.jsp'; }}] : frameQuery(selector);
assert.equal(nav(framed).kind, 'open', '自动跟随实际页面中的教务框架');
const ticketFrame = doc('i.nua.edu.cn');
const ticketQuery = ticketFrame.querySelectorAll.bind(ticketFrame);
ticketFrame.querySelectorAll = selector => selector === 'iframe[src],frame[src]'
  ? [{getAttribute() { return 'https://jwnew.nua.edu.cn/jsxsd/?ticket=ST-secret'; }}] : ticketQuery(selector);
assert.equal(nav(ticketFrame).kind, 'wait', '不可重复加载一次性登录票据地址');
class FakeEvent { constructor(type) { this.type = type; } }
class Control {
  get value() { return this._value; }
  set value(value) { this._value = value; }
  getClientRects() { return [1]; }
  getAttribute() { return '应用名称'; }
  dispatchEvent(event) { (this.events ||= []).push(event.type); }
}
function controls(page, selector, control) {
  control.ownerDocument = page;
  page.defaultView = {HTMLInputElement: Control, HTMLSelectElement: Control, Event: FakeEvent, KeyboardEvent: FakeEvent};
  const original = page.querySelectorAll.bind(page);
  page.querySelectorAll = query => query === selector ? [control] : original(query);
}
const paginationPage = doc('jwnew.nua.edu.cn', [table([headers, data])]);
paginationPage.body.innerText = '个人课表 课程名称 上课时间';
const pageSize = new Control(); pageSize.tagName = 'SELECT'; pageSize.value = '10';
pageSize.options = [{text: '10条', value: '10'}, {text: '全部', value: '-1'}];
controls(paginationPage, 'select', pageSize);
assert.equal(nav(paginationPage).action, '显示完整课表', '先自动展开全部记录');
assert.equal(pageSize.value, '-1');
assert(pageSize.events.includes('change'));
assert.equal(nav(paginationPage).kind, 'ready');
const semesterSelect = new Control(); semesterSelect.tagName = 'SELECT'; semesterSelect.value = '2026-2027-1';
semesterSelect.options = [{text: '2026-2027学年第一学期', value: '2026-2027-1'}, {text: '全部', value: '-1'}];
const semesterPage = doc('jwnew.nua.edu.cn', [table([headers, data])]);
semesterPage.title = '学期课表'; semesterPage.body.innerText = '学期课表 课程名称 上课时间';
controls(semesterPage, 'select', semesterSelect);
assert.equal(nav(semesterPage).kind, 'ready', '不能把学期选项全部误认为分页全部');
assert.equal(semesterSelect.value, '2026-2027-1');
const searchPage = doc('i.nua.edu.cn'); searchPage.title = '我的门户'; searchPage.body.innerText = '我的门户';
const search = new Control(); search.tagName = 'INPUT';
controls(searchPage, 'input[placeholder]', search);
assert.equal(nav(searchPage).action, '查找教务系统（新）');
assert.equal(search.value, '教务');
assert(search.events.includes('input') && search.events.includes('keydown'));
assert.equal(nav(searchPage).kind, 'wait', '搜索不循环提交');
const blank = doc('i.nua.edu.cn'); blank.body.innerText = ''; blank.title = '';
assert.equal(nav(blank).kind, 'blank', '识别持续空白以便有限恢复');
const ssoForm = doc('i.nua.edu.cn'); ssoForm.body.innerText = ''; ssoForm.title = '';
const formQuery = ssoForm.querySelectorAll.bind(ssoForm);
ssoForm.querySelectorAll = selector => selector === 'form,iframe,frame,input,button,a' ? [{}] : formQuery(selector);
assert.equal(nav(ssoForm).kind, 'wait', '保留正在自动提交的隐藏SSO表单，不误报空白');
const autoScript = fs.readFileSync(path.join(__dirname, '../app/src/main/java/cn/nua/wenguan/schedule/ScheduleSyncActivity.java'), 'utf8');
// Semester grid examples mirror the screenshot structure, with synthetic courses/teachers only.
const weekdayHeaders = ['', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六', '星期日'];
const gridCell = '文化产业学院\n测试课程\n(测试课程 1班)\n周老师\n6-8(周)[02-03-04节]\n文101';
const matrix = table([weekdayHeaders, ['第一二节\n08:00-09:40', '', gridCell, '', '', '', '', ''],
  ['第三四节\n10:00-11:40', '', gridCell, '', '', '', '', '']]);
const matrixPage = doc('jwnew.nua.edu.cn', [matrix]);
matrixPage.title = '学期课表'; matrixPage.body.innerText = '学期课表 星期一 星期二 星期三 星期四 星期五 星期六 星期日';
const matrixResult = JSON.parse(run(extract, matrixPage));
assert.equal(matrixResult.rows.length, 1, '跨时间格重复课程只读取一条');
assert.equal(matrixResult.rows[0][0], '测试课程');
assert.equal(matrixResult.rows[0][1], '6-8周,周二,第2-4节', '明确节次优先于第一二节行标签');
assert.equal(matrixResult.rows[0][2], '文101');
assert.equal(matrixResult.rows[0][3], '周老师', '姓周不是周次');
assert(matrixResult.warnings.length >= 2);
const repeatedBlocks = table([weekdayHeaders, ['第一二节', '', gridCell + '\n----------\n文化产业学院\n测试课程\n(测试课程 1班)\n周老师\n9(周)[03-04节]\n校外课堂', '', '', '', '', '']]);
const multiGrid = doc('jwnew.nua.edu.cn', [repeatedBlocks]); multiGrid.title = '学期课表';
assert.equal(JSON.parse(run(extract, multiGrid)).rows.length, 2, '单格多个周次/地点分段不能丢失');
const gapCell = table([weekdayHeaders, ['第一二节', '', gridCell.replace('02-03-04', '02-04'), '', '', '', '', '']]);
assert.equal(JSON.parse(run(extract, doc('jwnew.nua.edu.cn', [gapCell]))).rows[0][1], '6-8周,周二,第2节;6-8周,周二,第4节', '显式不连续节次不能补成连续');
const rowspanGrid = table([weekdayHeaders, ['第一二节', '', gridCell, '', '', '', '', ''],
  ['第三四节', '', '', '', '', '', '']]);
rowspanGrid.rows[1].children[2].rowSpan = 2;
rowspanGrid.rows[2].children[2].innerText = gridCell;
const rowspanResult = JSON.parse(run(extract, doc('jwnew.nua.edu.cn', [rowspanGrid])));
assert(rowspanResult.rows.some(r => r[1].includes('周三')), 'rowspan占位后下一格必须仍对应星期三');
const exportControl = node('导出课表为xls'), exportList = node('列表视图');
const exportGrid = doc('jwnew.nua.edu.cn', [matrix], [exportControl, exportList]);
exportGrid.title = '学期课表'; exportGrid.body.innerText = matrixPage.body.innerText;
assert.equal(nav(exportGrid).kind, 'export', '周视图先点击实际导出按钮');
assert.equal(exportControl.clicks, 1);
assert.equal(exportList.clicks, 0, '有导出按钮先不切换列表');
assert.equal(nav(exportGrid).kind, 'export_wait', '等待导出期间不立即导入页面');
assert.equal(exportControl.clicks, 1, '导出只请求一次，不循环下载');
const fallbackNav = JSON.parse(vm.runInNewContext(navigate, {window: {document: exportGrid, frames: [], __nuaAllowPageFallback: true}, URL}));
assert.equal(fallbackNav.kind, 'ready', '导出失败可显式恢复页面读取，不重走菜单');
assert.equal(exportList.clicks, 0, '备用周视图已有数据，不再点击列表导致刷新');
const gradeExport = node('导出课表');
const gradePage = doc('jwnew.nua.edu.cn', [matrix], [gradeExport]); gradePage.title = '成绩查询'; gradePage.body.innerText = '成绩查询';
assert.notEqual(nav(gradePage).kind, 'export', '不点击其他业务的导出');
assert.equal(gradeExport.clicks, 0);
const homeExport = node('导出课表');
assert.notEqual(nav(homePage([homeExport, node('我的课表')])).kind, 'export', '仅首页菜单不足以导出');
assert.equal(homeExport.clicks, 0);
const casExport = node('导出课表');
assert.equal(nav(doc('c.nua.edu.cn', [matrix], [casExport])).kind, 'login');
assert.equal(casExport.clicks, 0);
// 1.1.8 screenshot: child frame says 学期理论课表, and the actual control says only 导出.
const shortExport = node('导出');
shortExport.tagName = 'INPUT'; shortExport.innerText = ''; shortExport.value = '导出'; shortExport.type = 'button';
shortExport.form = {method: 'post'}; shortExport.getAttribute = () => null;
const theoryFrame = doc('jwnew.nua.edu.cn', [matrix], [shortExport]);
theoryFrame.title = ''; theoryFrame.body.innerText = '学期理论课表 周次 学年学期';
const theoryParent = homePage([node('我的课表'), node('• 学期课表')]);
assert.equal(nav(theoryParent, [{document: theoryFrame, frames: [], frameElement: node('课表框架')}]).kind, 'export', '框架中的短按钮导出也必须优先点击');
assert.equal(shortExport.clicks, 1, 'type=button不能误当成POST提交而跳过');
assert.equal(nav(theoryParent, [{document: theoryFrame, frames: []}]).kind, 'export_wait', '不重复导出或跳回个人中心');
assert.equal(JSON.parse(run(extract, theoryFrame)).rows.length, 1, '学期理论课表也可备用读取');
const plainSubmit = node('导出'); plainSubmit.tagName = 'INPUT'; plainSubmit.type = 'submit';
plainSubmit.form = {method: 'post'}; plainSubmit.getAttribute = () => null;
const submitGrid = doc('jwnew.nua.edu.cn', [matrix], [plainSubmit]); submitGrid.title = '学期理论课表';
assert.equal(nav(submitGrid).kind, 'ready', '未确认处理程序的默认POST提交不触发');
assert.equal(plainSubmit.clicks, 0);
const unknownExport = node('导出');
const unknownTable = doc('jwnew.nua.edu.cn', [table([['其他标题'], ['其他内容']])], [unknownExport]);
unknownTable.title = '学期理论课表';
assert.notEqual(nav(unknownTable).kind, 'export', '仅有课表标题，缺少真实课程结构，不泛化导出按钮');
assert.equal(unknownExport.clicks, 0);
const settings = doc('jwnew.nua.edu.cn');
settings.location.pathname = '/jsxsd/grsz/grsz_xgrxx.do'; settings.location.href = 'https://jwnew.nua.edu.cn' + settings.location.pathname;
settings.querySelector = () => { throw new Error('不能读取个人信息表单控件'); };
Object.defineProperty(settings, 'body', {get() {throw new Error('不能读取个人信息正文');}});
assert.equal(nav(settings).kind, 'login', '个人信息页面不进入自动脚本读取');
assert.equal(run(extract, settings), '', '个人信息页面不作为课表来源');
const inactiveLoginFrame = {frameElement: node('隐藏登录框架', false), get document() {throw new Error('隐藏页不应读取');}};
const inactiveProfileFrame = {frameElement: node('隐藏个人中心', false), get document() {throw new Error('隐藏页不应读取');}};
const hiddenResult = nav(readyPage, [inactiveLoginFrame, inactiveProfileFrame]);
assert.equal(hiddenResult.kind, 'ready', '已显示课表时忽略隐藏个人中心或登录标签');
assert.equal(hiddenResult.structure.hiddenFrames, 2);
assert.equal(hiddenResult.structure.frameErrors, 0);
const zeroFrame = node('零尺寸框架'); zeroFrame.getClientRects = () => [{width: 0, height: 0}];
assert.equal(nav(readyPage, [{frameElement: zeroFrame, get document() {throw new Error('零尺寸框架不应读取');}}]).structure.hiddenFrames, 1);
const cssFrame = node('不可见框架');
cssFrame.ownerDocument = {defaultView: {getComputedStyle() {return {visibility: 'hidden', display: 'block'};}}};
assert.equal(nav(readyPage, [{frameElement: cssFrame, get document() {throw new Error('CSS不可见框架不应读取');}}]).structure.hiddenFrames, 1);
assert.equal(JSON.parse(run(extract, trusted, [inactiveProfileFrame])).rows.length, 1);
const frozenShell = homePage();
const shellQuery = frozenShell.querySelectorAll.bind(frozenShell);
frozenShell.querySelectorAll = selector => selector === 'iframe[src],frame[src]'
  ? [{getAttribute() {return settings.location.href;}, getClientRects() {return [1];}}] : shellQuery(selector);
assert.equal(nav(frozenShell).kind, 'wait', '不能把xsMain个人中心iframe提升为主页面');
const portalProfile = doc('i.nua.edu.cn');
const portalProfileQuery = portalProfile.querySelectorAll.bind(portalProfile);
portalProfile.querySelectorAll = selector => selector === 'iframe[src],frame[src]'
  ? [{getAttribute() {return settings.location.href;}, getClientRects() {return [1];}}] : portalProfileQuery(selector);
assert.equal(nav(portalProfile).kind, 'wait', '门户也不自动打开个人资料框架');
const hiddenPortal = doc('i.nua.edu.cn');
const hiddenPortalQuery = hiddenPortal.querySelectorAll.bind(hiddenPortal);
hiddenPortal.querySelectorAll = selector => selector === 'iframe[src],frame[src]'
  ? [{getAttribute() {return 'https://jwnew.nua.edu.cn/jsxsd/framework/xsMain.jsp';}, getClientRects() {return [];}}] : hiddenPortalQuery(selector);
assert.equal(nav(hiddenPortal).kind, 'wait', '门户中的隐藏首页框架也不打开');
const encodedTicketPortal = doc('i.nua.edu.cn');
const encodedQuery = encodedTicketPortal.querySelectorAll.bind(encodedTicketPortal);
encodedTicketPortal.querySelectorAll = selector => selector === 'iframe[src],frame[src]'
  ? [{getAttribute() {return 'https://jwnew.nua.edu.cn/jsxsd/framework/xsMain.jsp?%74icket=ST-secret';}}] : encodedQuery(selector);
assert.equal(nav(encodedTicketPortal).kind, 'wait', '编码票据也不作框架提升');
const visibleProfileInShell = homePage([node('我的课表'), node('• 学期课表')]);
assert.equal(nav(visibleProfileInShell, [{document: settings, frames: [], frameElement: node('个人中心')}]).action,
  '点击：学期课表', '首页可见个人中心不读取其表单，但仍可走课表菜单');
assert(autoScript.includes('setDownloadListener'), '接收真实导出下载地址');
assert(autoScript.includes('SchoolSyncPolicy.canAutoOpenFrame(webView.getUrl(), action.optString("url"))'), '原生端也拒绝任意教务iframe提升');
assert(autoScript.includes('if (profileRecoveries++ == 0)'), '返回教务首页限一次，不循环个人信息页');
assert(autoScript.includes('ExportedXlsReader.parse(bytes)'), '下载文件必须经过格式校验');
const windowCode = autoScript.slice(autoScript.indexOf('onCreateWindow'), autoScript.indexOf('onCloseWindow'));
assert(!windowCode.includes('view.stopLoading()') && !windowCode.includes('view.onPause()'), '新窗口创建时不能中断SSO发送页面');
assert(autoScript.includes('onPageCommitVisible'), '新窗口应延后到页面可见时展示');
assert(autoScript.includes('if (!request.isForMainFrame())'), '不能误拦截隐藏SSO框架');
assert(!autoScript.includes('addJavascriptInterface'), '不添加原生JS桥接');
assert(windowCode.includes('for (WebView child : pending)'), '同入口不能重复创建未加载窗口');
assert(autoScript.includes('SchoolSyncPolicy.secureHttpFallback(url, request.getMethod())'), '受控HTTP入口只做GET的HTTPS尝试');
assert(autoScript.includes('guard.register(secure)'), 'SSO请求按完整安全目标判断，不用脱敏地址去重');
assert(!autoScript.includes('tried.add(SchoolSyncPolicy.displayAddress(url))'), '脱敏地址会把首次入口与认证回调错误折叠');
const duplicateBranch = autoScript.slice(autoScript.indexOf('if (decision == SchoolSyncPolicy.UpgradeResult.DUPLICATE)'), autoScript.indexOf('event("HTTPS入口转换或重复请求达到安全上限'));
assert(duplicateBranch.includes('return true;'), '重复请求本身必须取消');
assert(!/flowStopped\s*=|blockedNavigation\(|disposeWindow\(|stopLoading\(/.test(duplicateBranch), '取消重复不能打断HTTPS连接');
const retryCode = autoScript.slice(autoScript.indexOf('private void restartFlow()'), autoScript.indexOf('private void navigate('));
assert(retryCode.includes('manager.removeAllCookies'), '每次手动重新获取都必须清除旧Cookie');
assert(retryCode.includes('WebStorage.getInstance().deleteAllData()'), '清除网页本地存储');
assert(retryCode.includes('view.clearCache(true)') && retryCode.includes('view.clearHistory()'), '清除旧窗口缓存与历史');
assert(retryCode.indexOf('disposeWindow(view)') < retryCode.indexOf('manager.removeAllCookies'), '先销毁旧网页，再清Cookie');
assert(retryCode.includes('prepareRetry()') && autoScript.includes('private void prepareRetry() {\n        cancelFileRead();'), '重试取消旧下载和识别任务');
assert(retryCode.includes('boolean empty = !manager.hasCookies()') && retryCode.includes('loginReset.complete(token, empty)'), '回调确认无Cookie，不能把removed=false当失败');
assert(retryCode.includes('resetBrowser(true, PORTAL_URL)'), '清空成功后回学校入口，不复用教务Cookie');
assert(!retryCode.includes('ScheduleRuleStore.clearSchedule'), '清登录不删除已有课表');
assert(!autoScript.includes('hasAcademicCookie()'), '新同步不再复用旧账号Cookie');
const createCode = autoScript.slice(autoScript.indexOf('protected void onCreate'), autoScript.indexOf('private String asset'));
assert(createCode.includes('restartFlow();'), '用户打开同步入口才重新认证');
assert(createCode.includes('if (!getIntent().getBooleanExtra("manual_import", false))'), '手动导入不得触发清Cookie或学校网络入口');
assert(!autoScript.includes('SyncSettings') && !autoScript.includes('background_sync'), '不再保留登录或后台拉起认证');
assert(autoScript.includes('loginReset.takeReady(active)'), '后台清空完成只在前台恢复时开新登录页');
assert(autoScript.includes('loginReset.invalidate()'), '销毁后旧Cookie回调不能打开新页面');
assert(autoScript.includes('startDate[0] = LocalDate.of(year, month + 1, day);'), '导入保留选中的具体日期');
assert(autoScript.includes('ScheduleRepository.firstWeekText(startDate[0])'), '导入显示所选日期所在第一周');
assert(!autoScript.includes('errorHandler.proceed()'), '不能绕过证书检查');
assert(!autoScript.includes('MIXED_CONTENT_ALWAYS_ALLOW'), '不能全部放开明文子资源');
assert(autoScript.includes('if (++academicLoginCount >= 6)') && autoScript.includes('attempts.fail()'), 'SSO短暂过渡给予六次等待，失败仍计入统一双通道上限');
// External-network reproduction: a WebVPN academic shell/frame must use the same export pipeline.
const vpnPrefix = '/https/0123456789abcdef0123456789abcdefabcdef0123456789';
function viaVpn(page, academicPath) {
  page.location = {protocol: 'https:', hostname: 'v.nua.edu.cn', port: '',
    pathname: vpnPrefix + academicPath, href: 'https://v.nua.edu.cn' + vpnPrefix + academicPath};
  return page;
}
const vpnHome = viaVpn(homePage([node('我的课表'), node('• 学期课表')]), '/jsxsd/framework/xsMain.jsp');
const vpnExport = node('导出'); vpnExport.tagName = 'INPUT'; vpnExport.type = 'button';
vpnExport.form = {method: 'post'}; vpnExport.getAttribute = () => null;
const vpnFrame = viaVpn(doc('jwnew.nua.edu.cn', [matrix], [vpnExport]), '/jsxsd/xskb/xskb_list.do');
vpnFrame.title = ''; vpnFrame.body.innerText = '学期理论课表 周次 学年学期';
const vpnFrames = [{document: vpnFrame, frames: [], frameElement: node('课表框架')}];
assert.equal(nav(vpnHome, vpnFrames).kind, 'export', '现场故障回归：外网首页中的学期理论课表框架必须触发导出');
assert.equal(vpnExport.clicks, 1);
assert.equal(nav(vpnHome, vpnFrames).kind, 'export_wait', '外网导出不能重复点击');
assert.equal(nav(vpnHome, vpnFrames).academicHome, true, '网关首页也按可见菜单确认');
const vpnFallback = JSON.parse(vm.runInNewContext(navigate, {window: {document: vpnHome,
  frames: vpnFrames, __nuaAllowPageFallback: true}, URL}));
assert.equal(vpnFallback.kind, 'ready', '网关导出不可用时也允许周视图备用读取');
assert.equal(JSON.parse(run(extract, vpnHome, vpnFrames)).rows.length, 1, '外网框架周视图可以解析');
const vpnTree = nuaTermTree(true); viaVpn(vpnTree.page, '/jsxsd/framework/xsMain.jsp');
assert.equal(nav(vpnTree.page).action, '点击：学期课表');
assert.equal(nav(vpnTree.page).kind, 'wait', '外网叶子请求后不能收起父菜单');
assert.equal(vpnTree.mine.clicks, 0); assert.equal(vpnTree.cultivated.clicks, 0);
assert(vpnTree.offBranch.every(n => n.clicks === 0));
const vpnPassword = viaVpn(homePage(), '/jsxsd/framework/xsMain.jsp'); vpnPassword.password = true;
assert.equal(nav(vpnPassword).kind, 'sso_required', '网关里的教务密码页同样不可操作');
const vpnProfile = viaVpn(doc('jwnew.nua.edu.cn', [matrix], [node('导出')]), '/jsxsd/grsz/grsz_xgrxx.do');
vpnProfile.querySelector = () => {throw new Error('不可访问个人信息控件');};
Object.defineProperty(vpnProfile, 'body', {get() {throw new Error('不可读取个人信息正文');}});
assert.equal(nav(vpnProfile).kind, 'login'); assert.equal(run(extract, vpnProfile), '');
for (const path of ['/https/*/jsxsd/xskb/xskb_list.do', '/http/' + vpnPrefix.split('/')[2] + '/jsxsd/xskb/xskb_list.do',
    vpnPrefix + '/jsxsd/xskb/../grsz/test', vpnPrefix + '/jsxsd%2Fxskb/test']) {
  const invalidExport = node('导出'), invalid = viaVpn(doc('jwnew.nua.edu.cn', [matrix], [invalidExport]), '/jsxsd/xskb/test');
  invalid.location.pathname = path;
  assert.notEqual(nav(invalid).kind, 'export'); assert.equal(invalidExport.clicks, 0);
}
const vpnCas = viaVpn(doc('v.nua.edu.cn', [matrix], [node('导出')]), '/cas/login');
assert.equal(nav(vpnCas).kind, 'login'); assert.equal(run(extract, vpnCas), '');
assert(autoScript.includes('getPackageInfo(getPackageName(), 0).versionName'), '诊断必须读取真实安装版本');
assert(!/getString\(R.string.app_name\) \+ " 1\./.test(autoScript), '诊断不能继续写死旧版号');
console.log('Web script validation passed: WebVPN shell/frame export and fallback, leaf navigation, profile/password/CAS protections, campus regressions and runtime version.');
