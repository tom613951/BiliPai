# BiliPai 一键全自动编译、打包、推送及 GitHub Release 发布脚本
#
# 前置条件：
#   1. JDK 21 + Android SDK
#   2. Miuix 依赖凭证（GitHub Packages，公开包也需鉴权）：
#      `~/.gradle/gradle.properties` 写入 gpr.user / gpr.key
#   3. release 签名：仓库根目录 `keystore.properties`（已 gitignore）
#      缺失时 release 变体产出未签名 APK，本脚本会直接报错退出。
#
# 用法：
#   pwsh -NoProfile -File scripts/release_one_click.ps1
#   pwsh -NoProfile -File scripts/release_one_click.ps1 -SkipBuild   # 跳过编译，只发已有产物
#
Param(
    [switch]$SkipBuild = $false,
    [switch]$SkipPush  = $false,
    [string]$Repo      = "tom613951/BiliPai"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Step($msg) { Write-Host $msg -ForegroundColor Green }
function Write-Warn($msg) { Write-Host $msg -ForegroundColor Yellow }
function Fail($msg) { Write-Host $msg -ForegroundColor Red; exit 1 }

# 脚本必须从仓库根目录执行（app/build.gradle.kts 与 keystore.properties 都在此）
if (-not (Test-Path "app/build.gradle.kts")) {
    Fail "请在仓库根目录执行本脚本（找不到 app/build.gradle.kts）"
}

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " 🚀 BiliPai 一键 Release 打包发布脚本" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# ---------- 前置检查 ----------

# 1) release 签名是否就绪：这是与原脚本最大的差异。
#    未配置时 assembleRelease 会静默产出未签名 APK，装不上，必须提前拦下。
if (-not (Test-Path "keystore.properties")) {
    Fail "缺少 keystore.properties（release 签名配置）。请先创建该文件后再运行，或改用 -SkipBuild 跳过编译。"
}

# 2) Miuix 依赖凭证
if (-not $env:GITHUB_TOKEN) {
    $env:GITHUB_TOKEN = (gh auth token 2>$null)
}
if (-not $env:GITHUB_ACTOR) {
    $env:GITHUB_ACTOR = "tom613951"
}

# ---------- 解析版本号 ----------

$buildGradle = Get-Content "app/build.gradle.kts" -Raw
if ($buildGradle -match 'versionName\s*=\s*"([^"]+)"') {
    $VersionName = $Matches[1]
} else {
    Fail "无法解析 app/build.gradle.kts 中的 versionName"
}

$Tag = "v$VersionName-personal"

# 交付物路径由 app/build.gradle.kts 的 export task 决定：outputs/bilipai/<variant>/
$ApkPath = "app/build/outputs/bilipai/release/BiliPai-$VersionName.apk"

Write-Host "当前应用版本号: $VersionName" -ForegroundColor Yellow
Write-Host "发布 Target Tag: $Tag" -ForegroundColor Yellow
Write-Host "交付产物路径  : $ApkPath" -ForegroundColor Yellow

# ---------- 编译 ----------

if (-not $SkipBuild) {
    Write-Step "`n[1/4] 编译 Release APK（R8 + 资源压缩）..."
    if (Test-Path $ApkPath) { Remove-Item $ApkPath -Force }
    & ".\gradlew.bat" :app:assembleRelease --build-cache --parallel
    if ($LASTEXITCODE -ne 0) {
        Fail "Gradle 编译失败，请检查上方报错。"
    }
} else {
    Write-Warn "`n[1/4] 已指定 -SkipBuild，跳过编译。"
}

if (-not (Test-Path $ApkPath)) {
    Fail "找不到编译产物: $ApkPath"
}

$ApkSize = [math]::Round((Get-Item $ApkPath).Length / 1MB, 1)
Write-Step "✅ APK 编译完成: $ApkPath ($ApkSize MB)"

# ---------- 签名校验 ----------
# 未签名的 APK 无法安装，这里用 apksigner 提前确认，避免发布坏包。
$buildTools = Get-ChildItem (Join-Path $env:LOCALAPPDATA "Android\Sdk\build-tools") -Directory -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending | Select-Object -First 1
if ($buildTools) {
    $apksigner = Join-Path $buildTools.FullName "apksigner.bat"
    $cert = & $apksigner verify --print-certs $ApkPath 2>&1 | Select-String "certificate DN"
    if ($LASTEXITCODE -ne 0) {
        Fail "APK 签名校验失败：产物未签名或签名损坏。请检查 keystore.properties 配置。"
    }
    Write-Step "✅ 签名校验通过: $cert"
} else {
    Write-Warn "⚠️  未找到 Android build-tools，跳过签名校验。"
}

# ---------- 推送与发布 ----------

if (-not $SkipPush) {
    Write-Step "`n[2/4] 推送 main 分支..."
    git push origin main
    if ($LASTEXITCODE -ne 0) { Fail "git push main 失败" }

    Write-Step "`n[3/4] 更新 Tag $Tag ..."
    git tag -f $Tag
    git push origin $Tag --force
    if ($LASTEXITCODE -ne 0) { Fail "git push tag 失败" }
} else {
    Write-Warn "`n[2/4][3/4] 已指定 -SkipPush，跳过推送。"
}

Write-Step "`n[4/4] 创建/更新 GitHub Release 并上传 APK..."
gh release view $Tag --repo $Repo >$null 2>&1
if ($LASTEXITCODE -eq 0) {
    gh release upload $Tag "$ApkPath#BiliPai-$VersionName.apk" --repo $Repo --clobber
    if ($LASTEXITCODE -ne 0) { Fail "上传 Release 资源失败" }
} else {
    $notes = "基于官方源码 ($VersionName) 的个人自用 Release 打包版（release 变体，R8 + 资源压缩，自签名）。"
    gh release create $Tag "$ApkPath#BiliPai-$VersionName.apk" --repo $Repo --title $Tag --notes $notes
    if ($LASTEXITCODE -ne 0) { Fail "创建 Release 失败" }
}

Write-Host "`n=========================================" -ForegroundColor Cyan
Write-Host " 🎉 全部完成！" -ForegroundColor Green
Write-Host " Release: https://github.com/$Repo/releases/tag/$Tag" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
