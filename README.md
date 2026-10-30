# 南艺电子课表

面向南京艺术学院学生的 Android 课表工具，支持 Android 8.0 及以上。个人开发项目，非学校官方应用。

**[下载最新 APK](https://github.com/HerenYU2007/NUA-Schedule-Android/releases/latest)** · [1.2.6 更新说明](RELEASE-NOTES-v1.2.6.md)

## 主要功能

- 登录学校教务获取课表，或手动导入学校导出的 XLS。
- 列表、竖条两种课表，左右滑动切周，点击课程编辑。
- 设置中可调整第1—12节的全局上课时间，课程、提醒和小组件同步使用。
- 自定义日程、时间冲突提示、上课提醒与下一节课倒计时。
- 今日／周课表 × 深色／白色，共四款桌面小组件。
- 深色模式、课表导出与备份；仅打开 App 时并行检查多个更新站点。

## 开始使用

1. 安装 APK，选择登录获取课表或导入 XLS。
2. 核对内容并选择开学日期，该日期所在周为第 1 周。
3. 在设置或桌面小组件列表中添加喜欢的组件。

账号密码只在学校官方页面填写，App 不保存原始密码，也不在后台自动登录。获取失败或取消导入不会覆盖已有课表。提醒与组件刷新可能受系统权限、省电策略和桌面兼容性影响。

<!-- APK-DOWNLOAD-STATS:START -->
## APK 下载趋势

![所有版本 APK 按日累计下载量](docs/downloads/apk-downloads.svg?v=1793385917659)

所有版本合并，按北京时间每天记录一个**累计总下载量**；约每5分钟检查，数值或日期变化时更新。只统计 APK，不计 ZIP、HAP 和更新清单。历史从首次采样开始记录，镜像缓存下载可能不完整。

[查看统计说明与历史数据](docs/downloads/README.md) · [查看最近检查记录](https://github.com/HerenYU2007/NUA-Schedule-Android/actions/workflows/apk-download-stats.yml)
<!-- APK-DOWNLOAD-STATS:END -->

## 源码与许可

源码公开，采用 [PolyForm Noncommercial 1.0.0](LICENSE)：允许非商业使用、修改和分享，须遵守许可及保留声明；商业用途请先联系维护者获取授权。它是非商用源码许可，不属于 OSI 定义的开源许可。第三方库保留原有许可，详见 [第三方组件](third_party/README.md) 和 [NOTICE](NOTICE)。

开发环境：JDK 17、Android SDK 35、Gradle 8.7；本机 E 盘构建可运行 `.\build-on-e.ps1`。[发布说明](GITHUB_PUBLISH.md) · [连续切周验证记录](DEVELOPMENT-1.2.3-FAST-PAGING.md)

## 协助开发

由 [HerenYU2007](https://github.com/HerenYU2007) 维护和发布，使用 **OpenAI Codex / ChatGPT** 辅助开发、排查、测试及文档整理，不代表 OpenAI 官方出品或背书。
