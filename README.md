<div align="center">

<img src="docs/images/233娘.jpeg" height="96" alt="BiliPai" />

# BiliPai (个人自用 Fork)

**基于官方 [jay3-yy/BiliPai](https://github.com/jay3-yy/BiliPai) 同步的第三方 Bilibili Android 客户端**

<p>
  <a href="https://github.com/tom613951/BiliPai/releases/latest">
    <img src="https://img.shields.io/badge/Release-v0.3.3--personal-007AFF?style=for-the-badge&logo=github" alt="最新 Release" />
  </a>
  <a href="https://github.com/jay3-yy/BiliPai">
    <img src="https://img.shields.io/badge/Upstream-jay3--yy%2FBiliPai-FF9500?style=for-the-badge&logo=github" alt="官方 Upstream" />
  </a>
</p>

</div>

---

## 📌 说明

本仓库是 [jay3-yy/BiliPai](https://github.com/jay3-yy/BiliPai) 的个人定制分支，持续跟随官方源码同步，并在其上叠加个人补丁后自行打包发布。

当前基于官方 **v0.3.3**（`608ef1d45`，versionCode 445）源码构建。

---

## 🛠️ 个人定制修改

1. **评论区二级回复遮罩与交互优化**
   - 移除展开二级回复时的灰色背景遮罩层：`resolveVideoCommentSheetHostScrimAlpha` 峰值透明度固定为 `0f`，播放器上方不再出现灰色蒙层。
   - 优化背景点击拦截：展开二级回复（`THREAD_DETAIL`）时不再拦截背景点击，也不会因点背景而关闭主面板。

2. **禁用云端自动编译**
   - 移除 `.github` 目录，避免第三方仓库触发 GitHub Actions 构建。

---

## 📦 APK 下载

在 [**Releases 页面**](https://github.com/tom613951/BiliPai/releases) 获取最新可安装包。

### 构建变体说明

本仓库使用自签名配置构建 **`release`** 变体：

| 变体 | 特性 |
| --- | --- |
| `release`（本仓库发布） | 启用 R8 代码压缩 + 资源压缩，使用自签名密钥库签名 |
| `smooth` | 继承 release 语义，`isDebuggable=false`，跳过 R8，用于本地快速验证 |
| `dev` | 同 release，包名带 `.dev` 后缀，用于并存测试 |

> ⚠️ 本仓库 APK 使用**自签名证书**（非官方密钥）。与官方版应用 ID 相同，**安装前需先卸载官方版本**，且后续版本升级须使用同一密钥库签名。

---

## 🔧 自行构建

前置条件：JDK 21、Android SDK。

Miuix 依赖位于 GitHub Packages，**即使是公开包也要求鉴权**。需在 `~/.gradle/gradle.properties` 中配置（切勿提交到仓库）：

```properties
gpr.user=<你的 GitHub 用户名>
gpr.key=<你的 GitHub Token，需含 read:packages 权限>
```

构建正式版还需在仓库根目录放置 `keystore.properties`（已在 `.gitignore` 中）：

```properties
storeFile=/absolute/path/to/your-release.jks
storePassword=<密码>
keyAlias=<别名>
keyPassword=<密码>
```

生成密钥库：

```bash
keytool -genkeypair -v -keystore release.jks -alias bilipai \
  -keyalg RSA -keysize 4096 -validity 10000
```

构建：

```bash
./gradlew :app:assembleRelease
```

产物位于 `app/build/outputs/apk/release/`。若缺少 `keystore.properties`，release 变体将回退为未签名（不影响 `debug` / `smooth` / `dev`）。

> ⚠️ 项目路径**不能包含非 ASCII 字符**（含中文的目录会导致 AGP 直接拒绝构建）。

---

## 🤝 向官方提交的修复

构建本仓库时发现并修复了上游的问题，**均已合入官方仓库**：

| 编号 | 类型 | 标题 | 状态 |
| --- | --- | --- | --- |
| [#882](https://github.com/jay3-yy/BiliPai/issues/882) | Issue | 干净 clone 无法构建：`.gitignore` 的 `**/build/` 静默排除了插件源码包 `com.android.purebilibili.build` | ✅ CLOSED |
| [#884](https://github.com/jay3-yy/BiliPai/pull/884) | PR | 补齐缺失的 `ComposeDetachedOwnerGuard` 插件源码 | ✅ MERGED |
| [#885](https://github.com/jay3-yy/BiliPai/pull/885) | PR | 让该插件在全新 CI runner（无 Gradle 缓存）上也能工作 | ✅ MERGED |

### #882 / #884

上游 v0.3.x 在 `app/build.gradle.kts` 中引用了 Gradle 插件 `ComposeDetachedOwnerGuard`，但 `.gitignore` 里的 `**/build/` 规则同时匹配了 Java 包目录 `com/android/purebilibili/build/`，导致插件源码被 git 静默忽略（`git add` 不报错也不提示），公开仓库中缺失该实现。任何干净 clone 都会在配置阶段直接失败。

修复方式：在 `**/build/` 之后补充否定规则放行该包目录，并补齐插件实现。

### #885

上述实现改为扫描 Gradle transform 缓存并就地改写其中的 `classes.jar`。全新 CI runner 没有该缓存，而插件任务是编译任务的前置依赖、会在缓存被填充之前执行，因此每次干净检出都失败：

```
[ComposeDetachedOwnerGuard] no compose-ui classes.jar found under
/home/runner/.gradle/caches; run a build once so the transform cache is
populated, then retry.
```

修复方式：改为直接从依赖解析取得 compose-ui AAR（`detachedConfiguration`，版本由 BOM 与解析图决定、不硬编码），补丁产物写入项目内构建目录，**不再读写全局 Gradle 缓存**。同时修掉了旧实现的两个副作用 —— 改写时丢失 jar 目录项（内层 `classes.jar` 从 1375 项掉到 1333），以及在 compose-ui 缺席时静默报成功而非显式失败。

> 说明：这两个修复现已属于上游代码，**本仓库不再携带任何插件相关改动**，与上游逐字节一致。

---

## 🔗 相关链接

- **官方上游**：[jay3-yy/BiliPai](https://github.com/jay3-yy/BiliPai)
- **本 Fork**：[tom613951/BiliPai](https://github.com/tom613951/BiliPai)

---

## ⚖️ 许可与归属

本项目基于官方开源项目二次构建，**全部核心功能与实现均归 [jay3-yy](https://github.com/jay3-yy/BiliPai) 及上游贡献者所有**，本仓库仅包含少量个人定制补丁，不改动任何核心业务逻辑。

请遵守原项目的开源许可与相关法律法规。使用本客户端观看、下载或分享内容时，请遵守对应平台规则。如涉及版权或权益问题，请联系本仓库处理。
