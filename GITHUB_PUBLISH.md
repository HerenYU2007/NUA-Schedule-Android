# GitHub 发布准备（未创建或上传仓库）

本次提供源码与脚本。仓库名尚未确定，所以 App 中默认不联网检查更新。创建仓库并上传首个 Release 后，在“我的 → 配置软件更新源”填写：

`https://github.com/OWNER/REPOSITORY/releases/latest/download/update.json`

## 源码上传

新建自己的 GitHub 仓库，将本项目根目录的干净源码上传（交付源码 ZIP 不含构建缓存、SDK、APK、私钥、登录 Cookie）。推荐先本地 `git init`，检查 `git status` 中无敏感文件，再添加 remote 和推送。这里不自动创建仓库、不执行 push。

## 两种签名，不可混淆

- APK 安装签名必须保持与已安装1.1.25相同，否则不能覆盖安装。现有调试 keystore 在项目 `.android-user-home` 中，**不要上传到 GitHub**，也不要丢失。以后升级到正式签名需要迁移方案，不能直接换密钥。
- 软件更新清单用单独的 RSA-3072 私钥签名。私钥位于 `E:\南艺电子课表_发布密钥\release-private.pem`，也不能上传。App 和源码只内置对应公钥 `app/src/main/assets/update-public.pem`。这不是游戏项目的密钥。
- CI 尚未配置；先在当前本机 E 盘构建与发布，避免 CI 自动生成另一把调试签名导致所有用户无法覆盖。

## 每次发布

1. 提高 `app/build.gradle.kts` 的 versionCode 和 versionName，补充更新日志，版本号不可重复发布不同内容。
2. 在本机执行 `build-on-e.ps1 --offline --no-daemon :app:lintDebug`，先完成所有测试。
3. 运行发布脚本（将 owner/repository 换成新仓库）：

```powershell
node tools/prepare-github-release.cjs owner/repository E:/南艺文管课表_交付文件/Release-v版本号 E:/南艺电子课表_发布密钥/release-private.pem RELEASE-NOTES-v版本号.md
```

4. 脚本自动从构建元数据读版本与包名、从第四参数文件读取更新说明，生成 APK、带签名的 update.json、SHA256SUMS.txt。说明修改后重新签名生成，不要手工改已签名 JSON。
5. 在 GitHub 创建正式 Release，标签 `v版本号`，上传以上3个附件，设为 Latest。不要上传源码里以外的本机缓存、账号资料、密钥。
6. App 配置更新源后，下次打开检测到更高版本，显示预览；用户确认才下载和打开系统安装器。授权“允许此来源安装”后仍需用户在系统确认安装。

## 客户端安全与已知边界

- 仅启动主界面时检查，无后台更新任务。手动检查也是用户主动触发。
- HTTPS，限制重定向、清单和APK大小，检查 RSA 签名、APK大小/SHA-256、包名、versionCode 和当前APK签名证书；拒绝回退版本。
- 只把校验后的单个私有APK通过临时只读 content URI 交给系统安装器，安装器最终验证 APK 签名。不会卸载或清空原课表、日程。
- 没有把游戏的78个镜像或动态网页抓取直接搬进来；当前使用配置的HTTPS更新源及其HTTPS重定向。GitHub连接不通则安全失败，下次打开再检查。可在确认镜像部署后增加经签名保护的镜像传输。
- 没有在线仓库，所以本次不能验证公网更新闭环；未连接真机，安装授权返回和安装界面仍需真机验证。强制换密钥、降级或发布错误清单不会被自动接受。
