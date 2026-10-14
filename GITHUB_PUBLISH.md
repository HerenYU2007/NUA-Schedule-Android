# GitHub发布与内置更新

仓库：https://github.com/HerenYU2007/NUA-Schedule-Android

仅打开App时自动检查，也保留手动检查。更新源已经写入源码，不读取旧的自定义URL设置，不需要用户输入。

1.2.2起，清单与APK传输入口分别按三轮实测成功次数和完整响应耗时中位数排序；包括GitHub官方备用。新动态节点及失败节点仍保留，优先序不是安全信任列表，不削弱校验。原gh-proxy.org仍保留，但不再无条件排第一。测速是本机网络快照，不会每次打开App遍历测速；报告见reports/UPDATE_ROUTE_BENCHMARK_20261014.md。

## 清单与下载入口

- 清单原始地址：https://github.com/HerenYU2007/NUA-Schedule-Android/releases/latest/download/update.json 。
- 目录：https://github.akams.cn/ ，只读取页面及本站 /_next/static/chunks/*.js 中的显式节点记录，不执行JavaScript。
- 缓存：应用私有目录 updates/akams-nodes.txt。目录失败使用缓存，始终合并文档全部78个内置节点（包含IDN域名），去重后依次尝试，校验成功即停止。
- 镜像URL格式：https://节点/https://github.com/HerenYU2007/NUA-Schedule-Android/releases/latest/download/update.json 。
- 官方备用：GitHub直连；https://api.github.com/repos/HerenYU2007/NUA-Schedule-Android/releases/latest 查询update.json附件API，以octet-stream下载；https://raw.githubusercontent.com/HerenYU2007/NUA-Schedule-Android/master/updates/update.json 及对应GitHub raw地址。
- APK：签名清单指定 https://github.com/HerenYU2007/NUA-Schedule-Android/releases/download/v版本/nua-schedule-版本.apk 。优先 https://gh-proxy.org/原始URL ，随后尝试全部目录/缓存/内置节点、原始URL、对应版本附件API。
- 普通成功路径不会尝试剩余节点。文件名和版本绑定，不用latest APK链接替换已签名版本；没有第三方节点获得发布权限。

## 传输与安装

12秒连接、20秒读取超时；HTTPS和最多6次请求（包括重定向），拒绝明文、非标准端口、私人网络地址。清单最多128KiB，APK最多200MiB。镜像失败只记录时间、阶段、主机和错误类型到最多64KiB的私有日志，不含参数、返回正文或登录资料。

APK断点用签名摘要和大小绑定，206必须对应正确Content-Range，200表示重新开始，连接失败保留断点，完整数据不匹配则当前节点清空重试一次。每次换节点仍验证最终大小和SHA-256。安装前检查包名、递增versionCode和当前安装签名；只能交给安卓系统确认安装，不复制游戏文件、不卸载、不清空课表和日程。

全部节点失效时安全失败，不做证书绕过、HTTP降级或后台更新。逐节点超时可能耗时较长。手机真实升级、系统授权返回及数据保留仍需真机测试。

## 两种密钥

APK使用现有安装签名；本机项目.android-user-home中的keystore必须安全保管，禁止上传或在另一环境自动生成不同签名。

清单RSA私钥在 E:\南艺电子课表_发布密钥\release-private.pem，禁止上传。源码和APK只含 app/src/main/assets/update-public.pem 公钥。禁止复用游戏项目密钥。

## 每次发布

1. 提高versionCode/versionName，补充日志，本机构建并验证。
2. 执行：
   node tools/prepare-github-release.cjs HerenYU2007/NUA-Schedule-Android E:/南艺文管课表_交付文件/Release-v版本 E:/南艺电子课表_发布密钥/release-private.pem RELEASE-NOTES-v版本.md
3. 执行：
   node tools/validate-release-artifacts.cjs E:/南艺文管课表_交付文件/Release-v版本
4. 推送干净源码，创建对应tag的正式Release，上传APK、update.json、SHA256SUMS.txt和源码ZIP，设为Latest。不要上传私钥、缓存或Cookie。
5. 发布成功后，把同一份签名update.json复制到仓库updates/update.json并推送，使raw备用入口同步。备份清单不得提前指向尚未发布的APK；不要手动编辑签名清单。
6. 不覆盖已经发布的同名APK或清单。本次1.1.26修订版仅单独本地交付，正式新Release使用1.2.0。

本实现依据用户提供的《最后一班岗-自动更新实现方案.md》的节点、缓存、续传和安全验证要求改写为Android APK流程，不使用游戏目录替换或静默安装。
