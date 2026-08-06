# BiliPai 一键全自动编译、打包、推送及 GitHub Release 发布脚本
Param(
    [switch]$SkipBuild = $false
)

$ErrorActionPreference = "Stop"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " 🚀 BiliPai 极速一键 Release 打包发布脚本 " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. 提取 app/build.gradle.kts 中的 versionName
$buildGradle = Get-Content "app/build.gradle.kts" -Raw
if ($buildGradle -match 'versionName\s*=\s*"([^"]+)"') {
    $VersionName = $Matches[1]
} else {
    Write-Error "无法解析 app/build.gradle.kts 中的 versionName"
}

$Tag = "v$VersionName-personal"
$ApkPath = "app/build/outputs/bilipai/release/BiliPai-$VersionName.apk"

Write-Host "当前应用版本号: $VersionName" -ForegroundColor Yellow
Write-Host "发布 Target Tag: $Tag" -ForegroundColor Yellow

# 2. 执行极速 Release 编译打包
if (-not $SkipBuild) {
    Write-Host "`n[1/3] 开始高效编译 Release APK..." -ForegroundColor Green
    & ".\gradlew.bat" :app:assembleRelease --build-cache --parallel
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Gradle 编译失败，请检查报错日志！"
    }
}

if (-not (Test-Path $ApkPath)) {
    Write-Error "找不到编译产物: $ApkPath"
}

Write-Host "✅ APK 编译完成: $ApkPath" -ForegroundColor Green

# 3. 提交与推送 main 分支及 Tag
Write-Host "`n[2/3] 推送代码与 Tag ($Tag) 到 GitHub 远程仓库..." -ForegroundColor Green
git push origin main
git tag -f $Tag
git push origin $Tag --force

# 4. 发布 GitHub Release
Write-Host "`n[3/3] 创建/更新 GitHub Release 并上传 APK..." -ForegroundColor Green
gh release view $Tag --repo tom613951/BiliPai >$null 2>&1
if ($LASTEXITCODE -eq 0) {
    gh release upload $Tag "$ApkPath#BiliPai-$VersionName.apk" --repo tom613951/BiliPai --clobber
} else {
    gh release create $Tag "$ApkPath#BiliPai-$VersionName.apk" --repo tom613951/BiliPai --title "$Tag" --notes "基于官方最新源码 ($VersionName) 的个人自用 Release 打包版"
}

Write-Host "`n=========================================" -ForegroundColor Cyan
Write-Host " 🎉 全部完成！" -ForegroundColor Green
Write-Host " Release URL: https://github.com/tom613951/BiliPai/releases/tag/$Tag" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
