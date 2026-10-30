# 手机小课堂安装包归档

这里保留各版本可直接安装的 APK，以及当时的源码、截图、验收记录和 SHA-256 校验文件。APK 已作为普通 Git 文件归档，克隆仓库即可获取。

| 版本 | 可安装 APK | 源码归档 | 主要内容 | 校验值 |
|---|---|---|---|---|
| **1.2（最新）** | [手机小课堂-1.2-debug.apk](手机小课堂-1.2-debug.apk) | [1.2 源码](手机小课堂-1.2-源码.zip) | 22 个非微信课程改为华为风格练习手机，补齐桌面到功能的完整路径；总计 34 课 | [SHA256SUMS-1.2.txt](SHA256SUMS-1.2.txt) |
| 1.1 | [手机小课堂-1.1-debug.apk](手机小课堂-1.1-debug.apk) | [1.1 源码](手机小课堂-1.1-源码.zip) | 新增微信 12 课，总计 34 课 | [SHA256SUMS-1.1.txt](SHA256SUMS-1.1.txt) |
| 1.0 | [手机小课堂-1.0-debug.apk](手机小课堂-1.0-debug.apk) | [1.0 源码](手机小课堂-1.0-源码.zip) | 手机基础使用 22 课 | [SHA256SUMS.txt](SHA256SUMS.txt) |

最新版还可从 [GitHub v1.2 发布页](https://github.com/xxmxst/androidOldUser/releases/tag/v1.2) 下载。发布页的 `phone-classroom-1.2.apk` 与本目录的 `手机小课堂-1.2-debug.apk` 内容完全一致，仅使用英文文件名方便下载。

## 安装与升级

1. 将 APK 下载到 Android 8.0 或以上的手机，打开文件并按系统提示安装。
2. 已安装旧版时，直接安装 1.2 覆盖升级；无需卸载，也无需清除应用数据。
3. 打开“手机小课堂”即可离线学习，不需要登录。

这三个试用 APK 的包名均为 `com.example.phoneclass`，签名证书相同。最新的 1.2 版已在华为 NOH-AN00（API 31）验证。测试包使用独立的 `.qa` 包名，不包含在本安装包归档中。

## 1.2 验收与截图

[验收记录](验收记录-1.2.md) · [桌面](desktop-1.2.png) · [WLAN](wlan-1.2.png) · [密码弹窗](wlan-password-1.2.png) · [相机权限](permission-1.2.png) · [字体预览](font-1.2.png) · [图库](gallery-1.2.png)

各版本原有校验文件保持不变。可用 PowerShell 的 `Get-FileHash -Algorithm SHA256` 或系统的 `sha256sum` 检查下载文件。
