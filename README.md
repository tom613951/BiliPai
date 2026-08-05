<div align="center">

<img src="docs/images/233娘.jpeg" height="96" alt="BiliPai" />

# BiliPai (个人自用 Fork 定制版)

**基于官方 [jay3-yy/BiliPai](https://github.com/jay3-yy/BiliPai) 源码同步的第三方 Bilibili Android 客户端**

<p>
  <a href="https://github.com/tom613951/BiliPai/releases/tag/v26.0805.1-personal">
    <img src="https://img.shields.io/badge/Release-v26.0805.1--personal-007AFF?style=for-the-badge&logo=github" alt="最新 Release 发布" />
  </a>
  <a href="https://github.com/jay3-yy/BiliPai">
    <img src="https://img.shields.io/badge/Upstream-jay3--yy%2FBiliPai-FF9500?style=for-the-badge&logo=github" alt="官方 Upstream 仓库" />
  </a>
</p>

</div>

---

## 📌 个人自用 Fork 说明

本仓库是基于官方开源项目 [`jay3-yy/BiliPai`](https://github.com/jay3-yy/BiliPai) 源码同步的个人定制分支。由于官方作者暂停通过 GitHub Releases 发布 APK（转移至群组发布），本项目直接基于官方 GitHub 最新 `v26.0805.1` (`26.0805.1`) 源码分支重新打包，并持续应用个人定制补丁。

---

## 🛠️ 个人定制修改内容 (Personal Patches)

1. **评论区二级回复遮罩与交互优化**
   - 移除展开二级回复时的灰色背景遮罩层（Scrim Alpha 调整为 `0f`）。
   - 优化背景点击拦截逻辑，展开二级回复面板时不再强制拦截背景页面点击操作，保持流畅互动。

2. **液态玻璃开关视觉修复**
   - 修复在开启液态玻璃（Liquid Glass）视觉效果时，自适应开关（Switch）控件的形状异常问题。

3. **同步构建与 GitHub Releases 发布**
   - 每次同步官方 GitHub 最新源码并应用个人补丁后，会在本仓库的 [GitHub Releases 页面](https://github.com/tom613951/BiliPai/releases) 中提供编译打包好的最新 APK。

---

## 📦 APK 下载与发布

可在本仓库的 [**Releases 页面**](https://github.com/tom613951/BiliPai/releases/tag/v26.0805.1-personal) 获取最新的可安装包：

| 安装包类型 | 说明 | 产物名称 |
| --- | --- | --- |
| **Release 正式版** | 推荐日常使用 | [`BiliPai-26.0805.1.apk`](https://github.com/tom613951/BiliPai/releases/tag/v26.0805.1-personal) |
| **Dev 开发版** | 包含开发调试日志与变体 | [`BiliPai-26.0805.1-dev.apk`](https://github.com/tom613951/BiliPai/releases/tag/v26.0805.1-personal) |

---

## 🔗 相关链接

- **官方 Upstream 源码仓库**：[jay3-yy/BiliPai](https://github.com/jay3-yy/BiliPai)
- **本 Fork 仓库**：[tom613951/BiliPai](https://github.com/tom613951/BiliPai)
- **最新 Release 页面**：[v26.0805.1-personal Releases](https://github.com/tom613951/BiliPai/releases/tag/v26.0805.1-personal)
