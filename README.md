<div align="center">

<img src="docs/images/233娘.jpeg" height="96" alt="BiliPai" />

# BiliPai (个人自用 Fork)

**基于官方 [jay3-yy/BiliPai](https://github.com/jay3-yy/BiliPai) 同步的第三方 Bilibili Android 客户端**

<p>
  <a href="https://github.com/tom613951/BiliPai">
    <img src="https://img.shields.io/badge/Source-v0.7.0-007AFF?style=for-the-badge&logo=github" alt="源码版本" />
  </a>
  <a href="https://github.com/jay3-yy/BiliPai">
    <img src="https://img.shields.io/badge/Upstream-jay3--yy%2FBiliPai-FF9500?style=for-the-badge&logo=github" alt="官方 Upstream" />
  </a>
</p>

</div>

---

## 项目说明

本仓库是 [jay3-yy/BiliPai](https://github.com/jay3-yy/BiliPai) 的个人定制分支，持续跟随官方源码同步，并在其上叠加个人补丁。

当前基于官方 **v0.7.0**（`cb0719388`，versionCode 538）源码。**只同步代码，不提供预编译包。**

---

## 个人定制改动

1. **评论区二级回复遮罩与交互优化**
   - 移除展开二级回复时的灰色背景遮罩层：`resolveVideoCommentSheetHostScrimAlpha` 峰值透明度固定为 `0f`，播放器上方不再出现灰色蒙层。
   - 优化背景点击拦截：展开二级回复（`THREAD_DETAIL`）时不再拦截背景点击，也不会因点背景而关闭主面板。

2. **禁用云端自动编译**
   - 移除 `.github` 目录，避免第三方仓库触发 GitHub Actions 构建。

---

## 安装包获取

> **注意**：本仓库不提供预编译 APK，请自行构建（见下方「自行构建」）。
> 历史版本仍可在 [Releases 页面](https://github.com/tom613951/BiliPai/releases) 下载。

本仓库只跟随上游同步源码并叠加个人补丁，构建由使用者自行完成。

### 构建变体说明

| 变体 | 特性 |
| --- | --- |
| `release` | 启用 R8 代码压缩 + 资源压缩，需自备密钥库签名 |
| `smooth` | 继承 release 语义，`isDebuggable=false`，跳过 R8，用于本地快速验证 |
| `dev` | 同 release，包名带 `.dev` 后缀，用于并存测试 |

> **注意**：自行构建的 APK 与官方版应用 ID 相同，**安装前需先卸载官方版本**。
> 若你之前安装过本仓库的历史预编译包（v0.6.4 及更早），自行构建时请沿用同一密钥库，
> 否则需先卸载再安装，否则会报 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`。

> **说明**：历史预编译包（v0.5.5 ~ v0.6.4）使用密钥库 `CN=BiliPai Personal`
> （SHA-256 `44:B5:F2:B9:...:4B`，有效期至 2054）。v0.3.9-personal 及更早版本
> 使用不同签名，从那些版本升级**必须先卸载**。

---

## 自行构建

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

> **注意**：Windows 路径必须用正斜杠 `/` 或双反斜杠 `\\`**，不能写单反斜杠。
> `java.util.Properties` 会把 `\` 当转义符，`C:\Users\...\release.jks` 会被解析成
> `C:Users...release.jks`（路径不成立），`hasReleaseKeystore` 随之为 `false`，
> release 变体**静默回退成未签名 APK**，`assembleRelease` 不报错但产物装不上。
> 正确写法：`storeFile=C:/Users/you/release.jks`。
> 排查手段：`./gradlew :app:signingReport | grep -A3 "Variant: release"`，
> 看到 `Config: release` 才算生效（`Config: none` 即路径没读到）。

生成密钥库：

```bash
keytool -genkeypair -v -keystore release.jks -alias bilipai \
  -keyalg RSA -keysize 4096 -validity 10000
```

构建：

```bash
./gradlew :app:assembleRelease
```

产物位于 `app/build/outputs/bilipai/release/BiliPai-<version>.apk`（上游自 0.4.x 起改由 export task 输出规范名）。若缺少 `keystore.properties`，release 变体将回退为未签名（不影响 `debug` / `smooth` / `dev`）。

> **提示**：只想要一个能用的包、不在意 R8 压缩？用 `./gradlew :app:assembleSmooth`
> 跳过 R8，构建时间约为 release 的一半，产物在 `outputs/bilipai/smooth/`。

> **提示**：内存吃紧时，`gradle.properties` 已配置为单 JVM 构建
> （`kotlin.compiler.execution.strategy=in-process` + `-Xmx8g`），
> 实测峰值约 8.7GB。若物理内存不足 16GB，把 `org.gradle.jvmargs` 的 `-Xmx8g`
> 降到 `-Xmx6g`、`org.gradle.workers.max` 从 8 降到 4。

> **限制**：项目路径不能包含非 ASCII 字符（含中文的目录会导致 AGP 直接拒绝构建）。

---

## 上游缺陷修复与贡献

构建本仓库时发现并修复了上游的问题，**均已合入官方仓库**：

| 编号 | 类型 | 标题 | 状态 |
| --- | --- | --- | --- |
| [#882](https://github.com/jay3-yy/BiliPai/issues/882) | Issue | 干净 clone 无法构建：`.gitignore` 的 `**/build/` 静默排除了插件源码包 `com.android.purebilibili.build` | CLOSED |
| [#884](https://github.com/jay3-yy/BiliPai/pull/884) | PR | 补齐缺失的 `ComposeDetachedOwnerGuard` 插件源码 | MERGED |
| [#885](https://github.com/jay3-yy/BiliPai/pull/885) | PR | 让该插件在全新 CI runner（无 Gradle 缓存）上也能工作 | MERGED |

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

### 后续：上游改用 AGP 原生插桩（v0.3.6）

我在 [#880](https://github.com/jay3-yy/BiliPai/issues/880) 下反馈过：上述实现虽然能产出正确的补丁 AAR，但**该产物没有被接入 `:app` 的编译 classpath**，因此运行时崩溃仍未消除。

上游在 v0.3.6（提交 `3608e0347`）采纳了这一结论，并选择了更彻底的方案 —— 放弃 AAR 补丁路线，改用 **AGP 原生 `AsmClassVisitorFactory` 插桩**：

```java
components.onVariants(components.selector().all(), (Action<Variant>) variant -> {
    variant.getInstrumentation().transformClassesWith(
            ComposeDetachedOwnerGuard.class, InstrumentationScope.ALL, parameters -> Unit.INSTANCE);
    variant.getInstrumentation().setAsmFramesComputationMode(
            FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS);
});
```

插件由 333 行精简至 33 行，`DetachedOwnerFallback.java` 随之删除。官方 CHANGELOG 的表述是「**修正旧补丁仅生成 AAR、未接入打包流程的问题**」。

该方案在编译期直接改写字节码，产物天然进入打包流程。本仓库构建时已验证生效：`Task :app:transformReleaseClassesWithAsm` 执行，`LayoutNodeSubcompositionsState.getOutOfFrameExecutor()` 中注入 `isAttached` 守卫，未附着时直接返回 `null` 而非抛出 `IllegalStateException`。

---

## 相关链接

- **官方上游**：[jay3-yy/BiliPai](https://github.com/jay3-yy/BiliPai)
- **本 Fork**：[tom613951/BiliPai](https://github.com/tom613951/BiliPai)

---

## 许可与归属

本项目基于官方开源项目二次构建，**全部核心功能与实现均归 [jay3-yy](https://github.com/jay3-yy/BiliPai) 及上游贡献者所有**，本仓库仅包含少量个人定制补丁，不改动任何核心业务逻辑。

请遵守原项目的开源许可与相关法律法规。使用本客户端观看、下载或分享内容时，请遵守对应平台规则。如涉及版权或权益问题，请联系本仓库处理。
