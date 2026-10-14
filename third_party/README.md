# JExcelAPI / JXL

本工程只使用未修改的 JExcelAPI 2.6.12 本地 JAR 读取 BIFF/OLE XLS；不执行公式、宏或嵌入对象，不使用写表 API，不引入可选 Log4J 依赖。

- 官方项目：https://sourceforge.net/projects/jexcelapi/
- 运行库：`../app/libs/jxl-2.6.12.jar`
- 对应源码：`jxl-2.6.12-sources.jar`
- 原始构件：https://repo.maven.apache.org/maven2/net/sourceforge/jexcelapi/jxl/2.6.12/
- 许可：LGPL 2.1 或更新版本，完整文本 `LGPL-2.1.txt`，APK 也附带许可与组件说明。

运行库与源码下载后均对照 Maven Central 提供的 SHA-1 校验。库没有本地修改。应用完整源码随交付提供，可解包库源码研究或修改、替换 `app/libs` 中的运行库，并按项目 README 用 Gradle 重建/重新链接。当前 APK 不做混淆或代码裁剪，开发者不会限制为了调试此库修改所需的逆向工程。重新构建需自行签名，不提供私有签名密钥。
