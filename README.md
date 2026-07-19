# SysApp Module Builder

一个 Android 应用：选择手机中已安装的应用，生成 Magisk / KSU / APatch 兼容模块，把选中的应用固定为系统应用。

## 功能

- 列出设备已安装应用（支持搜索、筛选、显示系统应用开关）
- 多选应用，每个应用单独选择安装方式：
  - **PRIV_APP**：放到 `/system/priv-app/<pkg>/`，特权系统应用（可申请 signature|privileged 权限）
  - **SYSTEM_APP**：放到 `/system/app/<pkg>/`，普通系统应用
- 编辑模块元数据（id / name / version / versionCode / author / description）
- 生成 Magisk/KSU/APatch 双格式兼容 zip 模块
- 自动保存到 `Download/MagicModules/`
- 一键调起 Magisk/KSU 管理器安装
- 完整包含 base.apk + 所有 split APK

## 技术栈

- Kotlin + Jetpack Compose
- Material 3（Material You 动态色彩）
- AndroidX Compose BOM 2024.10.00
- AGP 8.7.0 / Kotlin 2.0.21
- minSdk 26 / targetSdk 35

## 模块工作原理

生成的 zip 包含：

```
module.prop          # 模块元数据
customize.sh         # 安装时执行，设置文件权限
system/
  priv-app/<pkg>/    # PRIV_APP 模式
    base.apk
    split_*.apk
  app/<pkg>/         # SYSTEM_APP 模式
    base.apk
    split_*.apk
```

**覆盖安装逻辑**：模块只增加 `/system/app` 或 `/system/priv-app` overlay，不删除原 `/data/app`。重启后 Android 通常将原用户安装识别为 `UPDATED_SYSTEM_APP`，它仍具备系统应用身份。禁用或卸载模块后 overlay 消失，原 APK自然恢复为普通用户应用，因此 overlay 模式不需要 `service.sh` 或 `uninstall.sh`。

## 构建

需要 JDK 17 和 Android SDK 35。

```bash
./gradlew :app:assembleDebug
```

输出 APK：`app/build/outputs/apk/debug/app-debug.apk`

## 使用流程

1. 首页点击「新建模板」，在独立页面填写模板名称和模块元数据
2. 保存后返回首页，点击模板卡片进入应用列表
3. 选择应用，并为每个应用设置普通或特权系统应用模式
4. 点击「下一步」预览模块内容
5. 点击「生成模块」，等待生成完成
6. 在弹窗中点击「打开安装」，选择 Magisk/KSU 管理器导入 zip
7. 重启设备，overlay 由模块管理器挂载

## 权限说明

- `QUERY_ALL_PACKAGES`：列出已安装应用（Android 11+ 必须）
- `WRITE_EXTERNAL_STORAGE`（maxSdkVersion=28）：旧版写入 Download

由于使用 `QUERY_ALL_PACKAGES`，本应用不适合在 Google Play 上架，建议通过 sideload 或 F-Droid 分发。

## 已知限制

- 提取 `/data/app/<pkg>/base.apk` 在大部分设备上 world-readable，少数 OEM 可能限制，需 root 才能读取
