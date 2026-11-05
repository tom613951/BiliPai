<div align="center">

<img src="docs/images/233娘.jpeg" height="96" alt="BiliPai" />

# BiliPai（个人自用 Fork）

基于 [jay3-yy/BiliPai](https://github.com/jay3-yy/BiliPai) 同步的第三方 Bilibili Android 客户端

<a href="https://github.com/jay3-yy/BiliPai"><img src="https://img.shields.io/badge/upstream-jay3--yy%2FBiliPai-FF9500?style=flat-square" alt="Upstream" /></a>
<img src="https://img.shields.io/badge/source-v0.7.8%20(551)-007AFF?style=flat-square" alt="Source v0.7.8 (551)" />
<img src="https://img.shields.io/badge/license-GPLv3-FF3B30?style=flat-square" alt="GPLv3" />

</div>

## 说明

跟随上游同步源码，叠加少量个人补丁。**只同步代码，不提供预编译 APK。**

当前基线：上游 **v0.7.8**（`60d41e542`，versionCode 551）。

## 个人改动

| 改动 | 说明 |
| --- | --- |
| 评论区去遮罩 | 展开楼中楼时不再显示灰色蒙层，`resolveVideoCommentSheetHostScrimAlpha` 恒返回 `0f` |
| 楼中楼背景点击 | 二级回复展开时（`THREAD_DETAIL`）不拦截背景点击，也不会因点背景关闭主面板 |
| 禁用云端编译 | 移除 `.github`，避免第三方仓库触发 Actions |
| 单 JVM 构建 | Kotlin 编译在 Gradle 进程内执行，构建内存峰值从 11.5G 降至 8.1G |

## 自行构建

前置：JDK 21、Android SDK 37。

Miuix 依赖在 GitHub Packages，公开包也需鉴权。写入 `~/.gradle/gradle.properties`：

```properties
gpr.user=<GitHub 用户名>
gpr.key=<Token，需 read:packages 权限>
```

发布版还需仓库根目录的 `keystore.properties`（已在 `.gitignore`）：

```properties
storeFile=C:/path/to/release.jks
storePassword=<密码>
keyAlias=<别名>
keyPassword=<密码>
```

> `storeFile` 必须用正斜杠或双反斜杠。`java.util.Properties` 会把单反斜杠当转义符，路径解析失败时 release 会**静默产出未签名 APK**。
> 校验：`./gradlew :app:signingReport | grep -A3 "Variant: release"`，需显示 `Config: release`。

```bash
./gradlew :app:assembleRelease   # 产物 app/build/outputs/bilipai/release/
./gradlew :app:assembleSmooth    # 跳过 R8，耗时约减半，用于快速验证
```

内存不足 16GB 时，把 `org.gradle.jvmargs` 的 `-Xmx8g` 降到 `-Xmx6g`，`org.gradle.workers.max` 从 8 降到 4。

> 项目路径不能含非 ASCII 字符（中文目录会导致 AGP 拒绝构建）。

## 签名

自签名，与官方版应用 ID 相同，**安装前需卸载官方版本**。

v0.5.5 ~ v0.6.4 的历史预编译包使用密钥库 `CN=BiliPai Personal`（SHA-256 `44:B5:F2:B9:...:4B`，有效期至 2054）。沿用该密钥库可覆盖升级，否则需先卸载，否则报 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`。

## 向上游提交的修复

| 编号 | 内容 | 状态 |
| --- | --- | --- |
| [#882](https://github.com/jay3-yy/BiliPai/issues/882) | `.gitignore` 的 `**/build/` 误排除插件源码包，导致干净 clone 无法构建 | ✅ CLOSED |
| [#884](https://github.com/jay3-yy/BiliPai/pull/884) | 补齐缺失的 `ComposeDetachedOwnerGuard` 插件源码 | ✅ MERGED |
| [#885](https://github.com/jay3-yy/BiliPai/pull/885) | 让该插件在无 Gradle 缓存的 CI runner 上也能工作 | ✅ MERGED |

上游在 v0.3.6 改用 AGP 原生 `AsmClassVisitorFactory` 插桩，上述插件相关改动已全部回到上游，本仓库不再携带。

## 链接

- 上游：[jay3-yy/BiliPai](https://github.com/jay3-yy/BiliPai)
- 本仓库：[tom613951/BiliPai](https://github.com/tom613951/BiliPai)
- 历史 Release：[Releases](https://github.com/tom613951/BiliPai/releases)

## 许可

核心功能与实现归 [jay3-yy](https://github.com/jay3-yy/BiliPai) 及上游贡献者，本项目遵循 [GPLv3](LICENSE)。非官方客户端，与 BiliBili 无隶属关系。
