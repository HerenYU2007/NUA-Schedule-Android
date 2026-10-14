# APK 下载趋势说明

README折线显示所有版本合并后的APK累计总下载量，横轴为北京时间日期，每天一个点。不是每日新增量，也不是安装人数。

## 统计范围

- 读取本仓库全部已发布GitHub Release，包含预发布/测试版，不区分版本号。
- 分页读取所有Release及各自的附件，仅匹配已上传完成、文件名以`.apk`结尾的附件（不区分大小写）。
- 不统计`.zip`、`.hap`、`.json`、源码自动打包文件或`.apk.sha256`。
- 附件按GitHub asset ID去重。删除或替换APK后，保留该附件最后一次已采样的下载数，避免累计值丢失；删除前尚未被采样的新增下载无法恢复。
- 下载次数不是去重用户数；重复下载和测试下载不能据此识别。镜像若使用缓存、没有请求GitHub，统计可能遗漏。

## 按日和自动更新

- GitHub Actions约每5分钟检查一次，并支持手动运行及Release发布/修改/删除后检查。
- 同一天的新累计值替换当天的点，日期变化则增加一个点；当前日显示最新已记录值，不声称是当天最终值。
- 下载数和日期都未改变时，不制造重复提交。图中“最近数据变动”不代表最后检查时间；最后检查记录可在工作流页面查看。
- 图中显示最近30天，完整日历史保存在[history.json](history.json)。缺失日期不填零、不伪造数据；只连接相邻采样日。
- GitHub提供累计计数而不是逐次下载时间，不能恢复首次采样之前的每日曲线。第一个点为首次采样时现有APK累计值。
- 调度可能延迟，README图片也可能被缓存；数值变化时自动更换图片URL的版本参数减少旧缓存，但不承诺秒级推送。已经打开的README页面通常需要刷新。
- API报错、限流、分页不完整或历史损坏时，工作流失败并保留旧统计，不把失败当作零下载。

## 仓库维护

自动提交只修改`history.json`、`apk-downloads.svg`和README统计生成区；不会改应用源码/版本、上传APK或发布Release。需要仓库允许工作流的`contents: write`权限；若默认分支保护拒绝提交，工作流会失败，不绕过保护。

```powershell
node --test tools/apk-download-stats.test.cjs
node tools/apk-download-stats.cjs
```

脚本无需安装第三方依赖，只访问GitHub官方API，不下载APK或收集App用户信息。认证只使用工作流临时`GITHUB_TOKEN`，不写入仓库或README。Actions停用、权限失败或仓库长时间无活动时，应查看工作流状态并按GitHub提示恢复。

官方资料：[Release API](https://docs.github.com/en/rest/releases/releases)、[附件API](https://docs.github.com/en/rest/releases/assets)、[定时工作流限制](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#schedule)、[图片缓存](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/about-anonymized-urls)。
